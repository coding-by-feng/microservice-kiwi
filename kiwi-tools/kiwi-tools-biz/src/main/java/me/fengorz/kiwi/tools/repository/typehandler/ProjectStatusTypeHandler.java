package me.fengorz.kiwi.tools.repository.typehandler;

import me.fengorz.kiwi.tools.model.project.ProjectStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(ProjectStatus.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ProjectStatusTypeHandler extends BaseTypeHandler<ProjectStatus> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ProjectStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter == null ? null : parameter.getCode());
    }

    @Override
    public ProjectStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public ProjectStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public ProjectStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private ProjectStatus parse(String v) {
        if (v == null || v.isEmpty()) return null;
        ProjectStatus s = ProjectStatus.fromInput(v);
        if (s == null) return null;
        return s;
    }
}

