package br.com.acme.refund.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.acme.refund.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
	
	public List<Payment> findAll();
	
}
