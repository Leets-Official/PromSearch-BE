package com.promsearch.user.application.port.out.user;

import com.promsearch.user.domain.User;

public interface SaveUserPort {

    User create(User user);

    User update(User user);
}
