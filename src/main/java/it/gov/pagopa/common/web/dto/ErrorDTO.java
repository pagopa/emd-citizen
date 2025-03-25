package it.gov.pagopa.common.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ErrorDTO implements ServiceExceptionPayload {

  @NotBlank
  private String code;
  @NotBlank
  private String message;
}
