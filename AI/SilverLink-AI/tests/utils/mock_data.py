"""Mock data for testing"""

# Sample FAQ data
SAMPLE_FAQS = [
    {
        "id": 1,
        "category": "노인장기요양보험",
        "question": "노인 장기 요양 보험이 뭐야?",
        "answer": "노인장기요양보험은 고령이나 노인성 질병 등으로 일상생활을 혼자 수행하기 어려운 노인 등에게 신체활동 또는 가사활동 지원 등의 장기요양급여를 제공하는 사회보험제도입니다."
    },
    {
        "id": 2,
        "category": "노인장기요양보험",
        "question": "장기요양보험 신청 방법은?",
        "answer": "국민건강보험공단 지사를 방문하거나 우편, 팩스, 인터넷(www.longtermcare.or.kr)을 통해 신청할 수 있습니다. 신청서와 함께 의사소견서를 제출해야 합니다."
    },
    {
        "id": 3,
        "category": "노인장기요양보험",
        "question": "장기요양등급은 어떻게 나뉘나요?",
        "answer": "장기요양등급은 1등급부터 5등급까지 있으며, 인지지원등급도 있습니다. 1등급이 가장 중증이며, 등급에 따라 받을 수 있는 급여가 다릅니다."
    },
    {
        "id": 4,
        "category": "복지서비스",
        "question": "노인 복지 서비스에는 어떤 것이 있나요?",
        "answer": "노인 복지 서비스에는 경로당 운영, 노인일자리 지원, 노인돌봄서비스, 치매관리 지원, 노인학대 예방 등이 있습니다."
    },
    {
        "id": 5,
        "category": "복지서비스",
        "question": "독거노인 돌봄 서비스는 어떻게 신청하나요?",
        "answer": "주민센터나 국번없이 129(보건복지콜센터)로 연락하여 신청할 수 있습니다. 만 65세 이상 독거노인이 대상입니다."
    },
    {
        "id": 6,
        "category": "건강관리",
        "question": "노인 건강검진은 어떻게 받나요?",
        "answer": "만 66세 이상 노인은 국민건강보험공단에서 제공하는 생애전환기 건강검진을 받을 수 있습니다. 가까운 검진기관에서 무료로 받을 수 있습니다."
    },
    {
        "id": 7,
        "category": "건강관리",
        "question": "치매 검진은 무료인가요?",
        "answer": "만 60세 이상 어르신은 보건소에서 무료로 치매 선별검사를 받을 수 있습니다. 정밀검사가 필요한 경우 일부 본인부담금이 있을 수 있습니다."
    },
    {
        "id": 8,
        "category": "생활지원",
        "question": "노인 교통카드 발급은 어떻게 하나요?",
        "answer": "만 65세 이상 어르신은 주민센터에서 경로우대증을 발급받아 대중교통을 무료 또는 할인된 요금으로 이용할 수 있습니다."
    },
    {
        "id": 9,
        "category": "생활지원",
        "question": "노인 일자리 사업은 어떤 것이 있나요?",
        "answer": "공익활동, 사회서비스형, 시장형 사업단, 취업알선형 등 다양한 노인일자리 사업이 있습니다. 만 60세 이상이면 신청 가능합니다."
    },
    {
        "id": 10,
        "category": "생활지원",
        "question": "기초연금은 얼마나 받을 수 있나요?",
        "answer": "2024년 기준 단독가구는 최대 월 32만원, 부부가구는 최대 월 51만 2천원을 받을 수 있습니다. 소득인정액에 따라 차등 지급됩니다."
    }
]

# Sample Inquiry data
SAMPLE_INQUIRIES = [
    {
        "id": 1,
        "guardian_id": 1,
        "elderly_id": 1,
        "question": "할머니 약 복용 시간이 언제인가요?",
        "answer": "오전 8시, 오후 2시, 저녁 7시에 복용하시면 됩니다. 식후 30분 이내에 드시는 것이 좋습니다."
    },
    {
        "id": 2,
        "guardian_id": 1,
        "elderly_id": 1,
        "question": "할머니 병원 예약은 언제인가요?",
        "answer": "다음주 화요일 오전 10시에 서울대병원 정형외과 예약되어 있습니다."
    },
    {
        "id": 3,
        "guardian_id": 1,
        "elderly_id": 1,
        "question": "할머니가 좋아하시는 음식은 무엇인가요?",
        "answer": "호박죽, 미역국, 된장찌개를 좋아하십니다. 특히 호박죽은 매주 수요일마다 드시고 싶어하십니다."
    },
    {
        "id": 4,
        "guardian_id": 2,
        "elderly_id": 3,
        "question": "할아버지 물리치료 일정은?",
        "answer": "매주 월, 수, 금 오후 3시에 재활병원에서 물리치료를 받으십니다."
    },
    {
        "id": 5,
        "guardian_id": 2,
        "elderly_id": 3,
        "question": "할아버지 혈압약은 언제 드시나요?",
        "answer": "아침 식사 후 1정, 저녁 식사 후 1정 드시면 됩니다."
    }
]

# Guardian-Elderly relationships
GUARDIAN_ELDERLY_RELATIONS = {
    "valid": [
        {"guardian_id": 1, "elderly_id": 1},
        {"guardian_id": 1, "elderly_id": 2},
        {"guardian_id": 2, "elderly_id": 3},
        {"guardian_id": 3, "elderly_id": 4},
    ],
    "invalid": [
        {"guardian_id": 1, "elderly_id": 3},
        {"guardian_id": 2, "elderly_id": 1},
        {"guardian_id": 3, "elderly_id": 2},
    ]
}

# Sample chat requests
SAMPLE_CHAT_REQUESTS = [
    {
        "message": "노인 장기 요양 보험이 뭐야?",
        "thread_id": "guardian_1",
        "guardian_id": 1,
        "elderly_id": 1
    },
    {
        "message": "장기요양보험 신청은 어떻게 하나요?",
        "thread_id": "guardian_1",
        "guardian_id": 1,
        "elderly_id": 1
    },
    {
        "message": "할머니 약 복용 시간 알려주세요",
        "thread_id": "guardian_1",
        "guardian_id": 1,
        "elderly_id": 1
    }
]
