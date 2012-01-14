package com.linkedin.clustermanager.participant.statemachine;

import com.linkedin.clustermanager.messaging.handling.MessageHandler.ErrorCode;
import com.linkedin.clustermanager.messaging.handling.MessageHandler.ErrorType;

public class StateTransitionError
{
  private final Exception _exception;
  private final ErrorCode _code;
  private final ErrorType _type;

  public StateTransitionError(ErrorType type, ErrorCode code, Exception e)
  {
    _type = type;
    _code = code;
    _exception = e;
  }

  public Exception getException()
  {
    return _exception;
  }

  public ErrorCode getCode()
  {
    return _code;
  }
}
