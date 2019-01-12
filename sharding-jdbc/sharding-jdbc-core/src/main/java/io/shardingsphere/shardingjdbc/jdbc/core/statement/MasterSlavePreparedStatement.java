/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
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
 * </p>
 */

package io.shardingsphere.shardingjdbc.jdbc.core.statement;

import com.google.common.base.Preconditions;
import io.shardingsphere.core.constant.properties.ShardingPropertiesConstant;
import io.shardingsphere.core.routing.router.masterslave.MasterSlaveRouter;
import io.shardingsphere.shardingjdbc.jdbc.adapter.AbstractMasterSlavePreparedStatementAdapter;
import io.shardingsphere.shardingjdbc.jdbc.core.connection.MasterSlaveConnection;
import lombok.AccessLevel;
import lombok.Getter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * PreparedStatement that support master-slave.
 * 
 * @author zhangliang
 * @author panjuan
 */
@Getter
public final class MasterSlavePreparedStatement extends AbstractMasterSlavePreparedStatementAdapter {
    
    private final MasterSlaveConnection connection;
    
    @Getter(AccessLevel.NONE)
    private final MasterSlaveRouter masterSlaveRouter;
    
    private final Collection<PreparedStatement> routedStatements = new LinkedList<>();
    
    public MasterSlavePreparedStatement(final MasterSlaveConnection connection, final String sql) throws SQLException {
        this(connection, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }
    
    public MasterSlavePreparedStatement(final MasterSlaveConnection connection, final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
        this(connection, sql, resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }
    
    public MasterSlavePreparedStatement(
            final MasterSlaveConnection connection, final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        this.connection = connection;
        masterSlaveRouter = new MasterSlaveRouter(connection.getMasterSlaveDataSource().getMasterSlaveRule(),
                connection.getMasterSlaveDataSource().getShardingProperties().<Boolean>getValue(ShardingPropertiesConstant.SQL_SHOW));
        for (String each : masterSlaveRouter.route(sql)) {
            PreparedStatement preparedStatement = connection.getConnection(each).prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            routedStatements.add(preparedStatement);
        }
    }
    
    public MasterSlavePreparedStatement(final MasterSlaveConnection connection, final String sql, final int autoGeneratedKeys) throws SQLException {
        this.connection = connection;
        masterSlaveRouter = new MasterSlaveRouter(connection.getMasterSlaveDataSource().getMasterSlaveRule(),
                connection.getMasterSlaveDataSource().getShardingProperties().<Boolean>getValue(ShardingPropertiesConstant.SQL_SHOW));
        for (String each : masterSlaveRouter.route(sql)) {
            PreparedStatement preparedStatement = connection.getConnection(each).prepareStatement(sql, autoGeneratedKeys);
            routedStatements.add(preparedStatement);
        }
    }
    
    public MasterSlavePreparedStatement(final MasterSlaveConnection connection, final String sql, final int[] columnIndexes) throws SQLException {
        this.connection = connection;
        masterSlaveRouter = new MasterSlaveRouter(connection.getMasterSlaveDataSource().getMasterSlaveRule(),
                connection.getMasterSlaveDataSource().getShardingProperties().<Boolean>getValue(ShardingPropertiesConstant.SQL_SHOW));
        for (String each : masterSlaveRouter.route(sql)) {
            PreparedStatement preparedStatement = connection.getConnection(each).prepareStatement(sql, columnIndexes);
            routedStatements.add(preparedStatement);
        }
    }
    
    public MasterSlavePreparedStatement(final MasterSlaveConnection connection, final String sql, final String[] columnNames) throws SQLException {
        this.connection = connection;
        masterSlaveRouter = new MasterSlaveRouter(connection.getMasterSlaveDataSource().getMasterSlaveRule(),
                connection.getMasterSlaveDataSource().getShardingProperties().<Boolean>getValue(ShardingPropertiesConstant.SQL_SHOW));
        for (String each : masterSlaveRouter.route(sql)) {
            PreparedStatement preparedStatement = connection.getConnection(each).prepareStatement(sql, columnNames);
            routedStatements.add(preparedStatement);
        }
    }
    
    @Override
    public ResultSet executeQuery() throws SQLException {
        Preconditions.checkArgument(1 == routedStatements.size(), "Cannot support executeQuery for DDL");
        return routedStatements.iterator().next().executeQuery();
    }
    
    @Override
    public int executeUpdate() throws SQLException {
        int result = 0;
        for (PreparedStatement each : routedStatements) {
            result += each.executeUpdate();
        }
        return result;
    }
    
    @Override
    public boolean execute() throws SQLException {
        boolean result = false;
        for (PreparedStatement each : routedStatements) {
            result = each.execute();
        }
        return result;
    }
    
    @Override
    public void clearBatch() throws SQLException {
        Preconditions.checkArgument(1 == routedStatements.size(), "Cannot support clearBatch for DDL");
        routedStatements.iterator().next().clearBatch();
    }
    
    @Override
    public void addBatch() throws SQLException {
        Preconditions.checkArgument(1 == routedStatements.size(), "Cannot support addBatch for DDL");
        routedStatements.iterator().next().addBatch();
    }
    
    @Override
    public int[] executeBatch() throws SQLException {
        Preconditions.checkArgument(1 == routedStatements.size(), "Cannot support executeBatch for DDL");
        return routedStatements.iterator().next().executeBatch();
    }
    
    @Override
    public ResultSet getResultSet() throws SQLException {
        Preconditions.checkArgument(1 == routedStatements.size(), "Cannot support getResultSet for DDL");
        return routedStatements.iterator().next().getResultSet();
    }
    
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        Preconditions.checkArgument(1 == routedStatements.size(), "Cannot support getGeneratedKeys for DDL");
        return routedStatements.iterator().next().getGeneratedKeys();
    }
    
    @Override
    public int getResultSetHoldability() throws SQLException {
        return routedStatements.iterator().next().getResultSetHoldability();
    }
    
    @Override
    public int getResultSetConcurrency() throws SQLException {
        return routedStatements.iterator().next().getResultSetConcurrency();
    }
    
    @Override
    public int getResultSetType() throws SQLException {
        return routedStatements.iterator().next().getResultSetType();
    }
    
    @Override
    public boolean isAccumulate() {
        return false;
    }
}
