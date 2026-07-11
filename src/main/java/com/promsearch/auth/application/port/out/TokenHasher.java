package com.promsearch.auth.application.port.out;

public interface TokenHasher {

    String hash(String token);
}
