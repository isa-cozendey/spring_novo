package br.edu.ifc.bikes.exception;

public class LocacaoUnavailableException extends RuntimeException {
    public LocacaoUnavailableException(String message) {
        super(message);
    }
}
