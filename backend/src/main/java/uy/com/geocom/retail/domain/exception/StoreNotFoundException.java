package uy.com.geocom.retail.domain.exception;

/**
 * Exception thrown when a store is not found.
 */
public class StoreNotFoundException extends DomainException {

    public StoreNotFoundException(String message) {
        super(message);
    }
}
