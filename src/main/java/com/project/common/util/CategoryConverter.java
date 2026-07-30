package com.project.common.util;

import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.IncomeCategory;
import com.project.common.enums.SavingCategory;
import com.project.common.enums.RecordType;

public class CategoryConverter {
    public static String convert(RecordType type, String category) {
        return switch(type) {
            case EXPENSE ->
                    ExpenseCategory.valueOf(category)
                            .getCategoryName();
            case INCOME ->
                    IncomeCategory.valueOf(category)
                            .getCategoryName();
            case SAVING ->
                    SavingCategory.valueOf(category)
                            .getCategoryName();
        };
    }
}
