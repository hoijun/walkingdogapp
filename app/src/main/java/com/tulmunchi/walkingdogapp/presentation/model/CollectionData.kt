package com.tulmunchi.walkingdogapp.presentation.model

import com.tulmunchi.walkingdogapp.R

/**
 * Collection 관련 비즈니스 데이터
 */
object CollectionData {
    /**
     * Collection 정보 맵
     */
    val collectionMap: Map<String, CollectionInfo> by lazy {
        hashMapOf(
            // 001~010: 겨울, 숲, 기본 동물 친구들
            "001" to CollectionInfo(
                "001",
                "밥알 허스키",
                "이불 밖은 위험해... (귤 까줘)",
                R.drawable.collection_001
            ),
            "002" to CollectionInfo(
                "002",
                "밥알 용용이",
                "길 잃은 거 아님. 탐험 중임. 진짜임.",
                R.drawable.collection_002
            ),
            "003" to CollectionInfo(
                "003",
                "밥알 캠핑용",
                "다이어트는 내일부터... (마시멜로 뇸뇸)",
                R.drawable.collection_003
            ),
            "004" to CollectionInfo(
                "004",
                "밥알 시츄",
                "이거 내 애착인형임. 눈독 들이지 마!",
                R.drawable.collection_004
            ),
            "005" to CollectionInfo(
                "005",
                "밥알 사슴",
                "풀만 먹는데 왜 살찌지..? (억울)",
                R.drawable.collection_005
            ),
            "006" to CollectionInfo(
                "006",
                "밥알 루돌프",
                "산타 할아버지는 어디 가고 나만 일해..?",
                R.drawable.collection_006
            ),
            "007" to CollectionInfo(
                "007",
                "밥알 꿀꿀이",
                "한입만 달라고 하지 마라. (진지함)",
                R.drawable.collection_007
            ),
            "008" to CollectionInfo(
                "008",
                "밥알 아기코끼리",
                "코가 손이라 젓가락질 필요 없지롱!",
                R.drawable.collection_008
            ),
            "009" to CollectionInfo(
                "009",
                "밥알 북극여우",
                "사실 엉덩이 좀 시렵다... (달달달)",
                R.drawable.collection_009
            ),
            "010" to CollectionInfo(
                "010",
                "밥알 리트리버",
                "공 던져줘! (근데 주진 않을 거야)",
                R.drawable.collection_010
            ),

            // 011~020: 집, 정원, 숲속 친구들
            "011" to CollectionInfo(
                "011",
                "밥알 식집사냥",
                "풀 뜯어 먹는 거 아님.. 해충 잡는 중임.. (진짜임)",
                R.drawable.collection_011
            ),
            "012" to CollectionInfo(
                "012",
                "밥알 핑크토끼",
                "당근 싫어하는데 컨셉 때문에 먹는다...",
                R.drawable.collection_012
            ),
            "013" to CollectionInfo(
                "013",
                "밥알 쿼카",
                "카드값? 그게 뭐죠? 전 그냥 행복한데요? (해맑)",
                R.drawable.collection_013
            ),
            "014" to CollectionInfo(
                "014",
                "밥알 시바견",
                "이 뼈다귀는 제 겁니다. 불만 있으면 짖으세요.",
                R.drawable.collection_014
            ),
            "015" to CollectionInfo(
                "015",
                "밥알 퍼그",
                "숨만 쉬어도 귀여운 게 죄라면 난 무기징역.",
                R.drawable.collection_015
            ),
            "016" to CollectionInfo(
                "016",
                "밥알 치즈냥",
                "이 실뭉치가 먼저 나한테 시비 걸었다냥!",
                R.drawable.collection_016
            ),
            "017" to CollectionInfo(
                "017",
                "밥알 참새",
                "일찍 일어나는 새가... 그냥 더 피곤하다. 짹.",
                R.drawable.collection_017
            ),
            "018" to CollectionInfo(
                "018",
                "밥알 아기공룡",
                "크앙! 나 무섭지? (제발 무섭다고 해줘)",
                R.drawable.collection_018
            ),
            "019" to CollectionInfo(
                "019",
                "밥알 사막여우",
                "내 귀가 큰 이유는... 니 욕하는 거 잘 들으려고.",
                R.drawable.collection_019
            ),
            "020" to CollectionInfo(
                "020",
                "밥알 비숑",
                "누가 인형이게? 못 맞추면 간식 압수!",
                R.drawable.collection_020
            ),

            // 021~030: 농장, 물속, 다양한 친구들
            "021" to CollectionInfo(
                "021",
                "밥알 삐약이",
                "내가 먼저냐 달걀이 먼저냐... 묻지 마라. (심오)",
                R.drawable.collection_021
            ),
            "022" to CollectionInfo(
                "022",
                "밥알 회색냥",
                "집사야, 밥그릇이 비었다. (방금 먹음)",
                R.drawable.collection_022
            ),
            "023" to CollectionInfo(
                "023",
                "밥알 고래",
                "나 살찐 거 아님. 그냥 물 많이 먹어서 부은 거임.",
                R.drawable.collection_023
            ),
            "024" to CollectionInfo(
                "024",
                "밥알 산타끼리",
                "굴뚝으로 들어가려다 낀 건... 비밀이다.",
                R.drawable.collection_024
            ),
            "025" to CollectionInfo(
                "025",
                "밥알 햄찌",
                "볼따구에 숨긴 거 없음. (사실 해바라기씨 30개 있음)",
                R.drawable.collection_025
            ),
            "026" to CollectionInfo(
                "026",
                "밥알 생쥐",
                "나는 요리 못해...",
                R.drawable.collection_026
            ),
            "027" to CollectionInfo(
                "027",
                "밥알 왕병아리",
                "치킨 아니라고! 튀기지 말라고!",
                R.drawable.collection_027
            ),
            "028" to CollectionInfo(
                "028",
                "밥알 코카",
                "공 던져! 아니, 뺏지는 말고! (모순 덩어리)",
                R.drawable.collection_028
            ),
            "029" to CollectionInfo(
                "029",
                "밥알 래서판다",
                "이건 위협하는 자세다. 만세 부르는 거 아님. 크앙!",
                R.drawable.collection_029
            ),
            "030" to CollectionInfo(
                "030",
                "밥알 포메라니안",
                "털 찐 거 아니야. 그냥 내가 찐 거야.",
                R.drawable.collection_030
            ),

            // 031~040: 야생 동물 친구들
            "031" to CollectionInfo(
                "031",
                "밥알 웰시코기",
                "내 다리가 짧은 게 아냐. 지구가 높은 거야.",
                R.drawable.collection_031
            ),
            "032" to CollectionInfo(
                "032",
                "밥알 하마",
                "수박? 한 입 컷이지. 껍질째 씹어주마.",
                R.drawable.collection_032
            ),
            "033" to CollectionInfo(
                "033",
                "밥알 코뿔소",
                "채식주의자라고 무시하지 마라. 들이받으면 아프다.",
                R.drawable.collection_033
            ),
            "034" to CollectionInfo(
                "034",
                "밥알 펭귄",
                "회는 역시 자연산이지. 초장 있나?",
                R.drawable.collection_034
            ),
            "035" to CollectionInfo(
                "035",
                "밥알 미어캣",
                "전갈 꼬치구이 맛집 찾는 중. (독은 뺐음)",
                R.drawable.collection_035
            ),
            "036" to CollectionInfo(
                "036",
                "밥알 붉은여우",
                "여우가 어떻게 우냐고? 그 노래 좀 제발 그만 불러...",
                R.drawable.collection_036
            ),
            "037" to CollectionInfo(
                "037",
                "밥알 비글",
                "이 뼈다귀는 이제 제 겁니다. 안방 침대에 숨겨야지!",
                R.drawable.collection_037
            ),
            "038" to CollectionInfo(
                "038",
                "밥알 다람쥐",
                "볼 빵빵한 거 살 아님. 미래를 위한 도토리 투자임.",
                R.drawable.collection_038
            ),
            "039" to CollectionInfo(
                "039",
                "밥알 코알라",
                "하루 20시간 수면은 필수. 깨우면 문다.",
                R.drawable.collection_039
            ),
            "040" to CollectionInfo(
                "040",
                "밥알 나무늘보",
                "나... 빠... 른... 데... 너... 네... 가... 급... 한... 거...",
                R.drawable.collection_040
            ),

            // 041~055: 환상, 맹수, 그리고 나머지 친구들
            "041" to CollectionInfo(
                "041",
                "밥알 죠스",
                "뚜루루뚜루~ 그 노래 부르면 저작권료 청구한다.",
                R.drawable.collection_041
            ),
            "042" to CollectionInfo(
                "042",
                "밥알 유니콘",
                "이 뿔 와이파이 안테나다. 비밀번호는 안 알려줌.",
                R.drawable.collection_042
            ),
            "043" to CollectionInfo(
                "043",
                "밥알 비버",
                "앞니 임플란트 아님. 100% 자연산 건치다.",
                R.drawable.collection_043
            ),
            "044" to CollectionInfo(
                "044",
                "밥알 치즈냥",
                "내가 훔친 거 아님.. 생선이 제발로 내 입에 들어온 거임..",
                R.drawable.collection_044
            ),
            "045" to CollectionInfo(
                "045",
                "밥알 암탉",
                "후라이 금지. 삶기 금지. 머랭 치기 금지. (엄근진)",
                R.drawable.collection_045
            ),
            "046" to CollectionInfo(
                "046",
                "밥알 원숭이",
                "바나나 먹으면 나한테 반하나? ...미안하다.",
                R.drawable.collection_046
            ),
            "047" to CollectionInfo(
                "047",
                "밥알 사자",
                "밀림의 왕이지만 너한텐 냥냥펀치만 날릴게.",
                R.drawable.collection_047
            ),
            "048" to CollectionInfo(
                "048",
                "밥알 호랑이",
                "판다 형님한테 대나무 뺏어왔다. 아삭아삭.",
                R.drawable.collection_048
            ),
            "049" to CollectionInfo(
                "049",
                "밥알 흰토끼",
                "맨날 당근만 주냐... 나도 가끔은 치킨 먹고 싶다.",
                R.drawable.collection_049
            ),
            "050" to CollectionInfo(
                "050",
                "밥알 퍼그",
                "이 줄은 내 생명줄이다. 절대 못 놓는다! 으르렁!",
                R.drawable.collection_050
            ),
            "051" to CollectionInfo(
                "051",
                "밥알 곰돌이",
                "이건 꿀이 아니라... 약이다. 아무튼 약이다.",
                R.drawable.collection_051
            ),
            "052" to CollectionInfo(
                "052",
                "밥알 고슴도치",
                "나 까칠한 거 컨셉이다. 사실 부드러운 남자다.",
                R.drawable.collection_052
            ),
            "053" to CollectionInfo(
                "053",
                "밥알 초코댕댕",
                "이 뼈다귀는 비상금이다. 아무도 못 준다! 으르렁!",
                R.drawable.collection_053
            ),
            "054" to CollectionInfo(
                "054",
                "밥알 북극곰",
                "얼죽아(얼어 죽어도 아이스) 회원 모집합니다.",
                R.drawable.collection_054
            ),
            "055" to CollectionInfo(
                "055",
                "밥알 대왕병아리",
                "털 찐 거라니까... 왜 자꾸 살쪘대! 삐약!",
                R.drawable.collection_055
            )
        )
    }
}