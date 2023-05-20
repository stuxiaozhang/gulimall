package com.atguigu.common.exception;

/**
 * ������ʹ�����Ϣ������
 * 1. �����붨�����Ϊ5Ϊ����
 * 2. ǰ��λ��ʾҵ�񳡾��������λ��ʾ�����롣���磺100001��10:ͨ�� 001:ϵͳδ֪�쳣
 * 3. ά�����������Ҫά�����������������Ƕ���Ϊö����ʽ
 * �������б�
 *  10: ͨ��
 *      001��������ʽУ��
 *  11: ��Ʒ
 *  12: ����
 *  13: ���ﳵ
 *  14: ����
 */
public enum BizCodeEnum {
    // �൱�ڸ������쳣����Ϣ��װ��һ��
    /**
     * ϵͳδ֪�쳣������쳣��
     */
    UNKNOWN_EXCEPTION(10000, "ϵͳδ֪�쳣"),
    /**
     * ����У���������У���쳣��
     */
    VALID_EXCEPTION(10001, "������ʽУ��ʧ��");

    private final int code;
    private final String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    public int getCode() {
        return code;
    }
    public String getMsg() {
        return msg;
    }
}
