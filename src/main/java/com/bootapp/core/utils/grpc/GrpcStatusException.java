package com.bootapp.core.utils.grpc;

import io.grpc.Status;

public class GrpcStatusException {
    public static RuntimeException GrpcInternalException(Exception e) {
        return Status.INTERNAL.withDescription("INTERNAL:"+e.getMessage()).withCause(e.getCause()).asRuntimeException();
    }
    public static RuntimeException GrpcInternalException(String s) {
        return Status.INTERNAL.withDescription("INTERNAL:"+s).asRuntimeException();
    }
    public static RuntimeException GrpcInvalidArgException(Exception e) {
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e.getCause()).asRuntimeException();
    }
    public static RuntimeException GrpcInvalidArgException(String s) {
        return Status.INVALID_ARGUMENT.withDescription(s).asRuntimeException();
    }
    public static RuntimeException GrpcAlreadyExistsException(Exception e) {
        return Status.ALREADY_EXISTS.withDescription(e.getMessage()).withCause(e.getCause()).asRuntimeException();
    }
    public static RuntimeException GrpcAlreadyExistsException(String s) {
        return Status.ALREADY_EXISTS.withDescription(s).asRuntimeException();
    }
    public static RuntimeException GrpcNotFoundException() {
        return Status.NOT_FOUND.withDescription("NON_EXISTS").asRuntimeException();
    }
}