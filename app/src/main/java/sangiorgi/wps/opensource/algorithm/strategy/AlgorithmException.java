package sangiorgi.wps.opensource.algorithm.strategy;

/** Exception thrown when an algorithm fails to generate a PIN */
public class AlgorithmException extends Exception {

  public AlgorithmException(String message) {
    super(message);
  }

  public AlgorithmException(String message, Throwable cause) {
    super(message, cause);
  }
}
