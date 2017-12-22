package com.dyh.transaction.dao.typehandler;

import com.dyh.transaction.enums.BaseEnum;
import com.dyh.transaction.enums.GlobalTransactionProcessExpectedStatusEnum;
import com.dyh.transaction.enums.GlobalTransactionProcessStatusEnum;
import com.dyh.transaction.enums.GlobalTransactionsStatusEnum;
import com.dyh.transaction.utils.EnumUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author dyh
 * @created at 2017 10 30 15:47
 */
@MappedTypes({
        GlobalTransactionsStatusEnum.class,
        GlobalTransactionProcessStatusEnum.class,
        GlobalTransactionProcessExpectedStatusEnum.class
})
public class ByteEnumTypeHandler<E extends Enum<?> & BaseEnum> extends BaseTypeHandler<BaseEnum> {
    private Class<E> clazz;

    public ByteEnumTypeHandler(Class<E> enumType) {
        if (enumType == null)
            throw new IllegalArgumentException("Type argument cannot be null");

        this.clazz = enumType;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, BaseEnum parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return EnumUtil.codeOf(clazz, rs.getByte(columnName));
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return EnumUtil.codeOf(clazz, rs.getByte(columnIndex));
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return EnumUtil.codeOf(clazz, cs.getByte(columnIndex));
    }
}