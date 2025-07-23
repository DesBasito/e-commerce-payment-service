package kg.manurov.ecommerce.handler;

import java.util.List;
import java.util.Map;

public record ErrorResponseBody(
        String title,
        Map<String, List<String>> errors
){}
