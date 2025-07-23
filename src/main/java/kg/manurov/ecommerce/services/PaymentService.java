package kg.manurov.ecommerce.services;

import kg.manurov.ecommerce.dto.PaymentRequest;
import kg.manurov.ecommerce.mapper.PaymentMapper;
import kg.manurov.ecommerce.model.Payment;
import kg.manurov.ecommerce.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import kg.manurov.ecommerce.notification.NotificationProducer;
import kg.manurov.ecommerce.notification.PaymentNotificationRequest;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final NotificationProducer notificationProducer;

    public Integer createPayment(PaymentRequest request) {
        Payment payment = this.repository.save(this.mapper.toPayment(request));

        this.notificationProducer.sendNotification(
                new PaymentNotificationRequest(
                        request.orderReference(),
                        request.amount(),
                        request.paymentMethod(),
                        request.customer().firstname(),
                        request.customer().lastname(),
                        request.customer().email()
                )
        );
        return payment.getId();
    }
}
