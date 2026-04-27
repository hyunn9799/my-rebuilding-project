package com.aicc.silverlink.global.config;

import com.aicc.silverlink.domain.welfare.dto.WelfareApiDto;
import com.aicc.silverlink.domain.welfare.dto.WelfareDetailResponse;
import com.aicc.silverlink.domain.welfare.dto.WelfareListResponse;
import com.aicc.silverlink.domain.welfare.entity.Source;
import com.aicc.silverlink.domain.welfare.entity.Welfare;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // 1. 공통 기본 설정 (엄격한 매칭)
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        // 2. 복지(Welfare) 관련 매핑 설정
        registerWelfareMappings(modelMapper);

        return modelMapper;
    }

    private void registerWelfareMappings(ModelMapper modelMapper) {

        // --- [1] 공통 컨버터 (Enum -> String) ---
        Converter<Source, String> sourceToString = ctx ->
                ctx.getSource() == null ? null : ctx.getSource().getDescription();

        // --- [2] 중앙부처 수집용 매핑 (CentralItem -> Welfare) ---
        modelMapper.createTypeMap(WelfareApiDto.CentralItem.class, Welfare.class)
                .addMappings(mapper -> {
                    // DTO의 intrsThemaArray -> Entity의 category로 매핑
                    mapper.map(WelfareApiDto.CentralItem::getIntrsThemaArray, Welfare::setCategory);
                    // 중앙부처는 jurMnofNm 이름이 같지만 명시적으로 매핑
                    mapper.map(WelfareApiDto.CentralItem::getJurMnofNm, Welfare::setJurMnofNm);
                });

        // --- [3] 지자체 수집용 매핑 (LocalItem -> Welfare) ---

        // (3-1) 지자체용 지역코드 합치기 (시도명 + 시군구명)
        Converter<WelfareApiDto.LocalItem, String> districtCodeConverter = new AbstractConverter<>() {
            @Override
            protected String convert(WelfareApiDto.LocalItem source) {
                return source.getCtpvNm() + " " + source.getSggNm();
            }
        };

        modelMapper.createTypeMap(WelfareApiDto.LocalItem.class, Welfare.class)
                .addMappings(mapper -> {
                    // 이름이 다른 필드들 수동 연결
                    mapper.map(WelfareApiDto.LocalItem::getBizChrDeptNm, Welfare::setJurMnofNm);     // 담당부서 -> 소관부처
                    mapper.map(WelfareApiDto.LocalItem::getIntrsThemaNmArray, Welfare::setCategory); // 관심주제 -> 카테고리

                    // 문의처 번호가 inqNum에 들어오는 경우가 많음 -> rprsCtadr(연락처)로 매핑
                    // (주의: 이미 값이 있는 경우 덮어쓰지 않도록 스킵 설정이 필요할 수 있으나, 여기선 기본 매핑)
                    mapper.map(WelfareApiDto.LocalItem::getInqNum, Welfare::setRprsCtadr);

                    // 커스텀 컨버터 적용
                    mapper.using(districtCodeConverter).map(src -> src, Welfare::setDistrictCode);
                });

        // --- [4] 조회용 응답 매핑 (Entity -> Response DTO) ---
        modelMapper.createTypeMap(Welfare.class, WelfareDetailResponse.class)
                .addMappings(mapper -> mapper.using(sourceToString).map(Welfare::getSource, WelfareDetailResponse::setSource));

        modelMapper.createTypeMap(Welfare.class, WelfareListResponse.class)
                .addMappings(mapper -> mapper.using(sourceToString).map(Welfare::getSource, WelfareListResponse::setSource));
    }
}