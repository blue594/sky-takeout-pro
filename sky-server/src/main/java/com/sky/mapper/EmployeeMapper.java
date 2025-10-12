package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    @Insert("insert into employee (name, username, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user) VALUES " +
            "(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{status},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    void insert(Employee employee);

    //TODO 这样写的作用是，(指XML文件内的内容)
    // 在有模糊查询的时候可以进行模糊查询，即使没有使用任何查询也会将所有数据按创建时间降序排序，
    // 而具体分页情况（如从第几条数据开始，每页有多少数据）则由之前写的PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize())决定
    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 全局或局部修改
     * @param employee
     */
    void update(Employee employee);

    /**
     * 根据ID获取员工信息
     * @param id
     * @return
     */
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);
}
