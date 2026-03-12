package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.Payment;
import com.trivine.llc.api.repository.projection.InvoiceRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT COALESCE(MAX(p.id), 0) + 1 FROM Payment p")
    Long findNextPaymentId();

//    @Query("SELECT p FROM Payment p JOIN p.company c JOIN c.loginUser lu WHERE lu.loginUserId = :loginUserId")
//    List<Payment> findPaymentsByCompanyLoginUserId(@Param("loginUserId") Long loginUserId);
//
//    @Query("SELECT p FROM Payment p WHERE " +
//            "LOWER(p.invoiceId) LIKE %:search% OR " +
//            "LOWER(p.company.companyName) LIKE %:search%")
//    Page<Payment> searchInvoices(@Param("search") String search, Pageable pageable);


    @Query("""
    select
      p.paymentId as paymentId,
      c.companyId as companyId,
      p.invoiceId as invoiceId,
      p.status as status,
      p.paymentDate as paymentDate,
      p.paymentAmount as paymentAmount,

      c.companyName as companyName,
      sm.suffix as suffix,
      c.streetAddress1 as streetAddress1,
      c.companyPhone1 as companyPhone1,
      c.state as state,
      lu.email as billToEmail,

      svc.serviceName as serviceName,
      cs.servicePrice as servicePrice
    from Payment p
    join p.company c
    join c.loginUser lu
    left join c.suffixMaster sm
    left join p.companyServices cs
    left join cs.serviceMaster svc
    where lu.loginUserId = :loginUserId
    order by p.paymentDate desc, p.paymentId desc
""")
    List<InvoiceRow> findInvoiceRows(@Param("loginUserId") Long loginUserId);


    @Query("""
  select p.paymentId
  from Payment p
  join p.company c
  join c.loginUser lu
  where (:search is null or :search = '' or
         lower(p.invoiceId) like concat('%', :search, '%') or
         lower(c.companyName) like concat('%', :search, '%') or
         lower(lu.email) like concat('%', :search, '%') or
         lower(p.status) like concat('%', :search, '%'))
  order by p.paymentDate desc, p.paymentId desc
""")
    Page<Long> searchPaymentIds(@Param("search") String search, Pageable pageable);

    @Query("""
  select p.paymentId
  from Payment p
  order by p.paymentDate desc, p.paymentId desc
""")
    Page<Long> findAllPaymentIds(Pageable pageable);



    @Query("""
  select
    p.paymentId as paymentId,
    c.companyId as companyId,
    p.invoiceId as invoiceId,
    p.status as status,
    p.paymentDate as paymentDate,
    p.paymentAmount as paymentAmount,

    c.companyName as companyName,
    sm.suffix as suffix,
    c.streetAddress1 as streetAddress1,
    c.companyPhone1 as companyPhone1,
    c.state as state,
    lu.email as billToEmail,

    svc.serviceName as serviceName,
    cs.servicePrice as servicePrice
  from Payment p
  join p.company c
  join c.loginUser lu
  left join c.suffixMaster sm
  left join p.companyServices cs
  left join cs.serviceMaster svc
  where p.paymentId in :ids
  order by p.paymentDate desc, p.paymentId desc, cs.companyServiceId asc
""")
    List<InvoiceRow> findInvoiceRowsByPaymentIds(@Param("ids") List<Long> ids);


    @Query("""
  select
    p.paymentId as paymentId,
    c.companyId as companyId,
    p.invoiceId as invoiceId,
    p.status as status,
    p.paymentDate as paymentDate,
    p.paymentAmount as paymentAmount,

    c.companyName as companyName,
    sm.suffix as suffix,
    c.streetAddress1 as streetAddress1,
    c.companyPhone1 as companyPhone1,
    c.state as state,
    lu.email as billToEmail,

    svc.serviceName as serviceName,
    cs.servicePrice as servicePrice
  from Payment p
  join p.company c
  join c.loginUser lu
  left join c.suffixMaster sm
  left join p.companyServices cs
  left join cs.serviceMaster svc
  where p.paymentId = :paymentId
  order by cs.companyServiceId asc
""")
    List<InvoiceRow> findInvoiceRowsByPaymentId(@Param("paymentId") Long paymentId);


}



