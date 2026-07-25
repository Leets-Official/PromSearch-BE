package com.promsearch.auth.application.port.out.crypto;

public interface HashTokenPort {

    String hash(String token);
}
