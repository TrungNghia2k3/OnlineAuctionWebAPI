package com.ntn.auction.mapper;

import com.ntn.auction.dto.response.CategoryResponse;
import com.ntn.auction.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    CategoryResponse toCategoryResponse(Category category);
}
