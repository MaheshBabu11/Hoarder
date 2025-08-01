package com.maheshbabu11.hoarder.core;


import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class CustomPhysicalNamingStrategy extends PhysicalNamingStrategyStandardImpl {

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        if (name == null || name.getText().isEmpty()) {
            return name;
        }
        // Preserve the original case and quotes
        return Identifier.toIdentifier(name.getText(), name.isQuoted());
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        if (name == null || name.getText().isEmpty()) {
            return name;
        }
        // Preserve the original case and quotes
        return Identifier.toIdentifier(name.getText(), name.isQuoted());
    }
}
