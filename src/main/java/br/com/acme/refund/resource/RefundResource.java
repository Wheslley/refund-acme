package br.com.acme.refund.resource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.acme.refund.iproperties.IProperties;
import br.com.acme.refund.model.OrderAcme;
import br.com.acme.refund.model.Payment;
import br.com.acme.refund.repository.OrderAcmeRepository;
import br.com.acme.refund.repository.PaymentRepository;

@RestController
public class RefundResource {

	@Autowired
	private OrderAcmeRepository orderAcmeRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	/***
	 * EndPoint do metodo de Refund
	 * 
	 * @param numOrder
	 * @return
	 */
	@RequestMapping(value = "/refund", method = RequestMethod.POST)
	public ResponseEntity<String> findAll(@RequestParam("num_order") Integer numOrder) {

		Optional<OrderAcme> optionalOrderAcme = this.orderAcmeRepository.findById(numOrder);
		long daysRefund = 0;
		
		//Verificacao de datas
		daysRefund = verifyDates(optionalOrderAcme, daysRefund);

		if (daysRefund > 10) {
			return new ResponseEntity<String>("Order pode ser cancelada!!", HttpStatus.OK);
		} else if (daysRefund == -1) {
			return new ResponseEntity<String>("Erro ao buscar a Order!!", HttpStatus.OK);
		} else {
			//verificacao de order e payment
			Integer auditing = auditingOrderAndPayment(optionalOrderAcme);
			if (auditing == 0) {
				return new ResponseEntity<String>("Order faturada anteriormente!!", HttpStatus.OK);
			} else if (auditing > 0) {
				return new ResponseEntity<String>("Order faturada!!", HttpStatus.OK);
			} else {
				return new ResponseEntity<String>("Erro ao buscar a Order ou Payment!!", HttpStatus.OK);
			}
		}

	}

	/***
	 * Auditoria dos Status da Order e Payment
	 * 
	 * @param optionalOrderAcme
	 */
	private Integer auditingOrderAndPayment(Optional<OrderAcme> optionalOrderAcme) {

		List<Payment> ListPayment = this.paymentRepository.findAll();
		Payment paymentAuditing = null;

		for (Payment payment : ListPayment) {
			if (payment.getOrder().getId() == optionalOrderAcme.get().getId()) {
				paymentAuditing = payment;
			}
		}

		if (paymentAuditing != null) {
			Integer validation = 0;
			
			//Verifica se o status da Order esta AGUARDANDO
			if (optionalOrderAcme.get().getStatus().equalsIgnoreCase(IProperties.AGUARDANDO)) {
				optionalOrderAcme.get().setStatus(IProperties.FATURADA);
				this.orderAcmeRepository.save(optionalOrderAcme.get());
				++validation;
			}
			
			//Verifica se o status do Payment esta AGUARDANDO
			if (paymentAuditing.getStatus().equalsIgnoreCase(IProperties.AGUARDANDO)) {
				paymentAuditing.setStatus(IProperties.FATURADA);
				paymentAuditing.setOrder(optionalOrderAcme.get());
				this.paymentRepository.save(paymentAuditing);
				++validation;
			}

			return validation;
		} else {
			return -1;
		}

	}

	/***
	 * Metodo responsavel por recuperar a data da Order e verificar se estourou o
	 * prazo para o reembolso.
	 * 
	 * @param optionalOrderAcme
	 * @param daysRefund
	 * @return
	 */
	private long verifyDates(Optional<OrderAcme> optionalOrderAcme, long daysRefund) {
		if (optionalOrderAcme == null) {
			return -1;
		} else {
			Calendar calendarOrder = Calendar.getInstance();
			calendarOrder.setTime(optionalOrderAcme.get().getData());

			// define datas
			LocalDateTime dataCadastro = LocalDateTime.of(calendarOrder.get(Calendar.YEAR),
					calendarOrder.get(Calendar.MONTH) + 1, calendarOrder.get(Calendar.DAY_OF_MONTH) + 1, 0, 0, 0);
			LocalDateTime hoje = LocalDateTime.now();

			// calcula diferença
			daysRefund = dataCadastro.until(hoje, ChronoUnit.DAYS);
		}
		return daysRefund;
	}

}
