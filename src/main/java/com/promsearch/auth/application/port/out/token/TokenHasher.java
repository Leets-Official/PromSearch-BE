package com.promsearch.auth.application.port.out.token;

public interface TokenHasher {

    String hash(String token);
}
