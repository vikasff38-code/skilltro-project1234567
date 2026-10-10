package org.skilltro;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException exception){
    Map<String,String> fields=new LinkedHashMap<>();
    exception.getBindingResult().getFieldErrors().forEach(error->fields.putIfAbsent(error.getField(),error.getDefaultMessage()));
    return ResponseEntity.badRequest().body(Map.of("message","Please check the submitted fields.","errors",fields));
  }
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Map<String,String>> malformedBody(){return ResponseEntity.badRequest().body(Map.of("message","The request body is missing or invalid."));}
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Map<String,String>> conflict(){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","This record conflicts with data already saved."));}
}
