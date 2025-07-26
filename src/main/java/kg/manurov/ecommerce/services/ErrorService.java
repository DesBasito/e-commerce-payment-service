package kg.manurov.ecommerce.services;

import jakarta.validation.ConstraintViolation;
import kg.manurov.ecommerce.handler.ErrorResponseBody;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartException;

import java.util.*;

@Service
public class ErrorService {
    public ErrorResponseBody makeResponse(IllegalStateException e) {
        String message = Optional.ofNullable(e.getMessage())
                .orElse("Встречено недопустимое состояние");

        return new ErrorResponseBody("Ошибка недопустимого состояния", Map.of("errors", List.of(message)));
    }


    public ErrorResponseBody makeResponse(IllegalArgumentException e) {
        String message = Optional.ofNullable(e.getMessage())
                .orElse("Предоставлен недопустимый аргумент");

        return new ErrorResponseBody("Ошибка недопустимого аргумента",Map.of("errors", List.of(message)));
    }

    public ErrorResponseBody makeResponse(DataIntegrityViolationException e) {
        String rootMessage = Optional.ofNullable(e.getRootCause())
                .map(Throwable::getMessage)
                .orElse(e.getMessage());

        String userFriendlyMessage = parseSqlMessage(rootMessage);

        return new ErrorResponseBody("Ошибка ограничения целостности данных", Map.of("errors", List.of(userFriendlyMessage)));
    }
    private String parseSqlMessage(String sqlMessage) {
        if (sqlMessage == null) return "Неизвестная ошибка базы данных";

        if (sqlMessage.contains("violates not-null constraint")) {
            String column = extractColumn(sqlMessage);
            return "Поле \"" + column + "\" не может быть пустым.";
        }

        if (sqlMessage.contains("duplicate key")) {
            return "Нарушено ограничение уникальности. Такая запись уже существует.";
        }

        return sqlMessage;
    }

    private String extractColumn(String msg) {
        int colIndex = msg.indexOf("column \"");
        if (colIndex >= 0) {
            int start = colIndex + 8;
            int end = msg.indexOf("\"", start);
            return msg.substring(start, end);
        }
        return "неизвестное поле";
    }




    public ErrorResponseBody makeResponse(MultipartException e) {
        String message = Optional.ofNullable(e.getMessage())
                .orElse("Недопустимый multipart-запрос или загрузка файла");

        return new ErrorResponseBody("Ошибка загрузки файла", Map.of("errors", List.of(message)));
    }

    public ErrorResponseBody makeResponse(Exception e) {
        String message = e.getMessage();
        return new ErrorResponseBody(message, Map.of("errors", List.of(message)));
    }

    public ErrorResponseBody makeResponse(BindingResult bindingResult) {
        Map<String, List<String>> reasons = new HashMap<>();
        bindingResult.getFieldErrors().stream()
                .filter(error -> error.getDefaultMessage() != null)
                .forEach(err -> {
                    if (!reasons.containsKey(err.getField())) {
                        reasons.put(err.getField(), new ArrayList<>());
                    }
                    reasons.get(err.getField()).add(err.getDefaultMessage());
                });
        return new ErrorResponseBody("Ошибка валидации" ,reasons);
    }

    public ErrorResponseBody makeResponse(Set<ConstraintViolation<?>> constraintViolations) {
        Map<String, List<String>> reasons = new HashMap<>();
        constraintViolations.forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            reasons.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        });

        return new ErrorResponseBody("Ошибка валидации" ,reasons);
    }
}
