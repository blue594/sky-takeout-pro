package com.sky.service;

import com.sky.result.Result;
import com.sky.vo.*;

import java.time.LocalDate;

public interface ReportService {
    TurnoverReportVO getTurnover(LocalDate begin, LocalDate end);

    UserReportVO getUserReport(LocalDate begin, LocalDate end);

    OrderReportVO getOrders(LocalDate begin, LocalDate end);

    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);
}
