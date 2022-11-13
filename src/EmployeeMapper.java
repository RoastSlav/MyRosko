import Anotations.*;

import java.util.List;

public interface EmployeeMapper {
    @Select("SELECT COUNT(*) FROM employees")
    int getNumberOfEmployees();

    @Select("SELECT * FROM employees")
    List<Employee> getAllEmployees();

    @Select("SELECT * FROM employees WHERE job_id = #{jobId}")
    List<Employee> getEmployeesByJob(int id);

    @Select("SELECT * FROM employees WHERE employee_id = #{employeeId}")
    Employee getEmployeeById(int id);

    @Insert("""
            INSERT INTO employees (first_name,
                                    last_name,
                                    email,
                                    phone_number,
                                    hire_date,
                                    job_id,
                                    salary,
                                    manager_id,
                                    department_id)
            VALUES (#{firstName},
                    #{lastName},
                    #{email},
                    #{phoneNumber},
                    #{hireDate},
                    #{jobId},
                    #{salary},
                    #{managerId},
                    #{departmentId}""")
    int addEmployee(Employee emp);

    @Update("""
            UPDATE employees SET (first_name = #{firstName},
                                    last_name = #{lastName},
                                    email = #{email},
                                    phone_number = #{phoneNumber},
                                    hire_date = #{hireDate},
                                    job_id = #{jobId},
                                    salary = #{salary},
                                    manager_id = #{managerId},
                                    department_id = #{departmentId}
            WHERE employee_id = #{id})
            """)
    int updateEmployee(Employee emp);

    @Update("""
            UPDATE employees SET salary = salary * #{percent + 1} WHERE salary < #{minSalary}
            """)
    int updateEmployeeSalaries(float percent, float minSalary);

    @Delete("""
            DELETE FROM employees WHERE employee_id = #{id}
            """)
    int deleteEmployee(int id);

    @Select("""
            SELECT FROM employees WHERE email = #{email},
                                        phone_number = #{phoneNumber},
                                        job_id = #{jobID}
            """)
    int getEmployeeId(Employee emp);
}
