import { useState, useEffect, useRef } from 'react';
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { mapApi } from "@/api/map";
import { WelfareFacilityResponse } from "@/types/api";
import { Search, Loader2, MapPin } from "lucide-react";

interface FacilityAutocompleteProps {
    value: string;
    onChange: (value: string) => void;
    onFacilitySelect?: (facility: WelfareFacilityResponse) => void;
    placeholder?: string;
    className?: string;
}

export default function FacilityAutocomplete({
    value,
    onChange,
    onFacilitySelect,
    placeholder = "시설명을 입력하세요",
    className = ""
}: FacilityAutocompleteProps) {
    const [suggestions, setSuggestions] = useState<WelfareFacilityResponse[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    
    const inputRef = useRef<HTMLInputElement>(null);
    const suggestionsRef = useRef<HTMLDivElement>(null);
    const debounceRef = useRef<NodeJS.Timeout>();

    // 검색 함수
    const searchFacilities = async (query: string) => {
        if (query.trim().length < 2) {
            setSuggestions([]);
            setShowSuggestions(false);
            return;
        }

        setIsLoading(true);
        try {
            const results = await mapApi.searchFacilities(query.trim());
            setSuggestions(results);
            setShowSuggestions(true);
            setSelectedIndex(-1);
        } catch (error) {
            console.error('시설 검색 실패:', error);
            setSuggestions([]);
        } finally {
            setIsLoading(false);
        }
    };

    // 디바운스된 검색
    const debouncedSearch = (query: string) => {
        if (debounceRef.current) {
            clearTimeout(debounceRef.current);
        }
        
        debounceRef.current = setTimeout(() => {
            searchFacilities(query);
        }, 300);
    };

    // 입력값 변경 처리
    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newValue = e.target.value;
        onChange(newValue);
        debouncedSearch(newValue);
    };

    // 시설 선택 처리
    const handleFacilitySelect = (facility: WelfareFacilityResponse) => {
        onChange(facility.name);
        setShowSuggestions(false);
        setSelectedIndex(-1);
        
        if (onFacilitySelect) {
            onFacilitySelect(facility);
        }
    };

    // 키보드 네비게이션
    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (!showSuggestions || suggestions.length === 0) return;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                setSelectedIndex(prev => 
                    prev < suggestions.length - 1 ? prev + 1 : 0
                );
                break;
            case 'ArrowUp':
                e.preventDefault();
                setSelectedIndex(prev => 
                    prev > 0 ? prev - 1 : suggestions.length - 1
                );
                break;
            case 'Enter':
                e.preventDefault();
                if (selectedIndex >= 0 && selectedIndex < suggestions.length) {
                    handleFacilitySelect(suggestions[selectedIndex]);
                }
                break;
            case 'Escape':
                setShowSuggestions(false);
                setSelectedIndex(-1);
                break;
        }
    };

    // 외부 클릭 시 자동완성 닫기
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (
                inputRef.current && 
                !inputRef.current.contains(event.target as Node) &&
                suggestionsRef.current &&
                !suggestionsRef.current.contains(event.target as Node)
            ) {
                setShowSuggestions(false);
                setSelectedIndex(-1);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    // 컴포넌트 언마운트 시 디바운스 타이머 정리
    useEffect(() => {
        return () => {
            if (debounceRef.current) {
                clearTimeout(debounceRef.current);
            }
        };
    }, []);

    return (
        <div className={`relative ${className}`}>
            <div className="relative">
                <Input
                    ref={inputRef}
                    value={value}
                    onChange={handleInputChange}
                    onKeyDown={handleKeyDown}
                    placeholder={placeholder}
                    className="pr-10"
                />
                <div className="absolute inset-y-0 right-0 flex items-center pr-3">
                    {isLoading ? (
                        <Loader2 className="w-4 h-4 animate-spin text-gray-400" />
                    ) : (
                        <Search className="w-4 h-4 text-gray-400" />
                    )}
                </div>
            </div>

            {/* 자동완성 제안 목록 */}
            {showSuggestions && suggestions.length > 0 && (
                <Card 
                    ref={suggestionsRef}
                    className="absolute z-50 w-full mt-1 max-h-60 overflow-y-auto border shadow-lg bg-white"
                >
                    <div className="p-1">
                        {suggestions.map((facility, index) => (
                            <div
                                key={facility.id}
                                className={`
                                    flex items-start gap-3 p-3 cursor-pointer rounded-md transition-colors
                                    ${index === selectedIndex 
                                        ? 'bg-blue-50 border-blue-200' 
                                        : 'hover:bg-gray-50'
                                    }
                                `}
                                onClick={() => handleFacilitySelect(facility)}
                            >
                                <MapPin className="w-4 h-4 text-blue-500 mt-0.5 flex-shrink-0" />
                                <div className="flex-1 min-w-0">
                                    <div className="font-medium text-sm text-gray-900 truncate">
                                        {facility.name}
                                    </div>
                                    <div className="text-xs text-gray-500 mt-1">
                                        {facility.typeDescription || facility.type}
                                    </div>
                                    <div className="text-xs text-gray-400 mt-1 truncate">
                                        {facility.address}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </Card>
            )}

            {/* 검색 결과 없음 */}
            {showSuggestions && !isLoading && suggestions.length === 0 && value.trim().length >= 2 && (
                <Card className="absolute z-50 w-full mt-1 border shadow-lg bg-white">
                    <div className="p-4 text-center text-sm text-gray-500">
                        검색 결과가 없습니다.
                    </div>
                </Card>
            )}
        </div>
    );
}