package com.rideshare.domain.unitofwork;

import java.sql.Connection;

public interface UnitOfWork {
    void begin();
    void commit();
    void rollback();
    Connection getConnection();
}
