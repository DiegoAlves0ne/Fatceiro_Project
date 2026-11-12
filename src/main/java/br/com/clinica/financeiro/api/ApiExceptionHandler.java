package br.com.clinica.financeiro.api;
import java.util.NoSuchElementException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail ausente(NoSuchElementException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,e.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class,ConstraintViolationException.class})
    ProblemDetail invalido(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflito(IllegalStateException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,e.getMessage()); }
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class,DataIntegrityViolationException.class})
    ProblemDetail concorrencia(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Conflito de atualização ou chave duplicada. Repita com a mesma chave.");
    }
}
