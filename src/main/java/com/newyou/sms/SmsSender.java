package com.newyou.sms;

/**
 * 문자(SMS) 발송 인터페이스.
 * application.properties 의 sms.provider 값에 따라 구현체가 선택됩니다.
 * - console : 개발용. 실제 문자 대신 서버 콘솔에 출력
 * - solapi : 솔라피(SOLAPI)를 통해 실제 휴대폰으로 문자 발송
 */
public interface SmsSender {

    /**
     * @param to   받는 사람 전화번호 (숫자만, 예: 01012345678)
     * @param text 문자 내용
     * @throws SmsSendException 발송 실패 시
     */
    void send(String to, String text);

    /**
     * 실제 휴대폰으로 문자가 가는지 여부.
     * false(콘솔 모드)이면 개발 편의를 위해 인증번호를 앱 화면에 보여줍니다.
     */
    default boolean deliversRealSms() {
        return true;
    }
}