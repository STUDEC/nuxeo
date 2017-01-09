/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class JDBCMapperTxSuspender implements InvocationHandler {

    protected final Mapper mapper;

    protected JDBCMapperTxSuspender(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = TransactionHelper.runInNewTransaction(() -> {
            try {
                return method.invoke(mapper, args);
            } catch (InvocationTargetException cause) {
                return cause.getCause();
            } catch (IllegalAccessException cause) {
                return cause;
            }
        });
        if (result instanceof Throwable) {
            throw (Throwable) result;
        }
        return result;
    }

    public static Mapper newConnector(Mapper mapper) {
        return (Mapper) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { Mapper.class }, new JDBCMapperTxSuspender(mapper));
    }

    public static Mapper unwrap(Mapper mapper) {
        if (!Proxy.isProxyClass(mapper.getClass())) {
            return mapper;
        }
        return ((JDBCMapperTxSuspender) Proxy.getInvocationHandler(mapper)).mapper;
    }
}