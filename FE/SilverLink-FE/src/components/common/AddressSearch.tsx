import { useEffect, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { MapPin } from 'lucide-react';

// Daum 주소 검색 SDK 타입 정의.
declare global {
    interface Window {
        daum: {
            Postcode: new (options: {
                oncomplete: (data: DaumPostcodeResult) => void;
                onclose?: () => void;
                width?: string | number;
                height?: string | number;
            }) => {
                open: () => void;
                embed: (container: HTMLElement) => void;
            };
        };
    }
}

export interface DaumPostcodeResult {
    zonecode: string; // 우편번호
    address: string; // 기본 주소 (도로명)
    addressEnglish: string;
    addressType: string;
    userSelectedType: string;
    roadAddress: string; // 도로명 주소
    roadAddressEnglish: string;
    jibunAddress: string; // 지번 주소
    jibunAddressEnglish: string;
    bcode: string; // 법정동 코드 (10자리)
    bname: string; // 법정동명
    bname1: string;
    bname2: string;
    sido: string; // 시도명
    sigungu: string; // 시군구명
    sigunguCode: string; // 시군구 코드
    roadname: string; // 도로명
    roadnameCode: string;
    buildingCode: string; // 건물관리번호
    buildingName: string;
    apartment: string;
    autoRoadAddress: string;
    autoJibunAddress: string;
    query: string;
}

export interface AddressData {
    address: string; // 도로명 주소
    jibunAddress: string; // 지번 주소
    zonecode: string; // 우편번호
    bcode: string; // 법정동 코드 (행정동 코드로 활용)
    sido: string;
    sigungu: string;
    bname: string;
}

interface AddressSearchProps {
    onSelect: (data: AddressData) => void;
    buttonText?: string;
    disabled?: boolean;
}

const AddressSearch = ({
    onSelect,
    buttonText = '주소 검색',
    disabled = false,
}: AddressSearchProps) => {
    // SDK 로드 확인
    useEffect(() => {
        // SDK가 이미 로드되어 있는지 확인
        if (window.daum?.Postcode) return;

        // SDK 스크립트 동적 로드
        const script = document.createElement('script');
        script.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
        script.async = true;
        document.head.appendChild(script);

        return () => {
            // Cleanup (optional)
        };
    }, []);

    const handleSearch = useCallback(() => {
        if (!window.daum?.Postcode) {
            alert('주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해주세요.');
            return;
        }

        new window.daum.Postcode({
            oncomplete: (data: DaumPostcodeResult) => {
                // 도로명 주소 우선, 없으면 지번 주소
                const address = data.roadAddress || data.jibunAddress || data.address;

                onSelect({
                    address,
                    jibunAddress: data.jibunAddress,
                    zonecode: data.zonecode,
                    bcode: data.bcode,
                    sido: data.sido,
                    sigungu: data.sigungu,
                    bname: data.bname,
                });
            },
        }).open();
    }, [onSelect]);

    return (
        <Button
            type="button"
            variant="outline"
            onClick={handleSearch}
            disabled={disabled}
            className="whitespace-nowrap"
        >
            <MapPin className="w-4 h-4 mr-2" />
            {buttonText}
        </Button>
    );
};

export default AddressSearch;
