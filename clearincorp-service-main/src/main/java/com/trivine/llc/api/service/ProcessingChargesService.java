package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.llc.request.ProcessingChargesDto;
import com.trivine.llc.api.entity.CompanyServices;
import com.trivine.llc.api.entity.ServiceMaster;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.repository.CompanyServiceRepository;
import com.trivine.llc.api.repository.ServiceMasterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessingChargesService {

    private final CompanyServiceRepository companyServiceRepository;
    private final ServiceMasterRepository serviceMasterRepository;

    @Transactional
    public ProcessingChargesDto getProcessingCharges(Long companyId) {
        BigDecimal totalCharges = BigDecimal.ZERO;
        ProcessingChargesDto processingChargesDto = new ProcessingChargesDto();


        List<CompanyServices> companyServices1 = companyServiceRepository.findActiveCompanyServices(companyId);

        if (!companyServices1.isEmpty()) {
            for (CompanyServices companyServices : companyServices1) {
                Long serviceId = companyServices.getServiceMaster().getServiceId();


                ServiceMaster serviceMaster = serviceMasterRepository.findByServiceId(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Service not found for ID: " + serviceId));

                String serviceName = serviceMaster.getServiceName();
                BigDecimal servicePrice = serviceMaster.getServicePrice();

                switch (serviceName) {
                    case "EIN Registration" -> processingChargesDto.setFileForEin(servicePrice);
                    case "Operating Agreement" -> processingChargesDto.setOperatingAgreement(servicePrice);
                    case "Expedit Process" -> processingChargesDto.setExpediteRequired(servicePrice);
                    case "State Fee" -> {
                        processingChargesDto.setStateFee(companyServices.getServicePrice());
                        totalCharges = totalCharges.add(companyServices.getServicePrice());
                    }
                    case "Registered Agent Fee" -> processingChargesDto.setRegisterAgentFee(companyServices.getServicePrice());
                }

                totalCharges = totalCharges.add(servicePrice);
            }
        }

        processingChargesDto.setTotalCharges(totalCharges);
        return processingChargesDto;
    }

}

