#include <jni.h>
#include <string>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdarg.h>
#include <fcntl.h>
#include <time.h>
#include <sys/utsname.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#define _BUILD_ANDROID_
#define LOG_TAG                    "rtpstream"
#if defined(_BUILD_ANDROID_)
//#include <utils/Log.h>
#include <android/log.h>
#define TAG "MY_TAG"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,    TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,     TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,     TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,    TAG, __VA_ARGS__)
//char err[] = "wrong";
//LOGE("Something went %s", err);
#define SRC_IP_ADDR				"127.0.0.1"
#define DEST_IP_ADDR            "192.168.98.118"
#else
#define SRC_IP_ADDR          "10.250.12.60"
#define DEST_IP_ADDR         "10.250.12.205"
#endif
//#define RTP_SIZE_MAX            (1460)
#define RTP_SIZE_MAX            (14600)
#define RTP_HEADER_SIZE        (12)
#define NALU_INDIC_SIZE        (4)
#define NALU_HEAD_SIZE        (1)
#define FU_A_INDI_SIZE        (1)
#define FU_A_HEAD_SIZE        (1)
#define PAYLOAD_SIZE_MAX        (65535)
#define DE_TIME                (3600) //90000 /25 fps =  3600
#define SRC_PORT                (5151)
#define DEST_PORT                (5004)
#define SINGLE_NALU_DATA_MAX    (RTP_SIZE_MAX - RTP_HEADER_SIZE)
#define SLICE_NALU_DATA_MAX        (RTP_SIZE_MAX - RTP_HEADER_SIZE - FU_A_INDI_SIZE-FU_A_HEAD_SIZE)
#define SLICE_SIZE                (RTP_SIZE_MAX - RTP_HEADER_SIZE)
#define MIN(a, b)                ( ((a)<(b)) ? (a) : (b) )
static unsigned short s_nSeqNum = 0;
static unsigned char s_pNalu_buffer[RTP_SIZE_MAX + 12];
static unsigned char s_pRtpbuffer[PAYLOAD_SIZE_MAX], s_pTempbuffer[PAYLOAD_SIZE_MAX];
static int s_unRtpbufferIndex = 0;
static unsigned int s_unTimestamp = 0;
static unsigned int s_unSsrc = 0;
static struct sockaddr_in s_stServAddrRtp;
static int isRunning = true;
//https://github.com/txgcwm/Linux-C-Examples/blob/master/rtsp/src/rtp_h264.c
typedef struct _RTP_header {
    /* byte 0 */
    unsigned char csrc_len: 4;
    unsigned char extension: 1;
    unsigned char padding: 1;
    unsigned char version: 2;
    unsigned char payload: 7;
    unsigned char marker: 1;
    /* bytes 2, 3 */
    unsigned short seq_no;
    /* bytes 4-7 */
    unsigned int timestamp;
    /* bytes 8-11 */
    unsigned int ssrc;                    /* stream number is used here. */
} RTP_header;
static void print_v(const char *fmt, va_list ap) {
#define    LOCAL_BUF_LEN    512
    char szBuf[LOCAL_BUF_LEN];
    int nWritten;
    char *pLog, *pGarbage = NULL;
    nWritten = vsnprintf(szBuf, LOCAL_BUF_LEN, fmt, ap);
    if ((nWritten < 0) || (LOCAL_BUF_LEN <= nWritten)) {
        pLog = (char *) malloc(4096);
        pGarbage = pLog;
        if (pLog == NULL) {
            return;
        }
        vsnprintf(pLog, 4096, fmt, ap);
    } else {
        pLog = szBuf;
    }
#if defined(_BUILD_ANDROID_)
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "%s", pLog);
#else
    printf("%s",pLog);
#endif
    if (pGarbage != NULL) {
        free(pGarbage);
        pGarbage = NULL;
    }
}
void print(const char *pFormat, ...) {
    va_list ap;
    va_start(ap, pFormat);
    print_v(pFormat, ap);
    va_end(ap);
}
static int build_rtp_header(RTP_header *r) {
    r->version = 2;
    r->padding = 0;
    r->extension = 0;
    r->csrc_len = 0;
    r->marker = 0;
    r->payload = 96;
    r->seq_no = htons(s_nSeqNum);
    s_unTimestamp += DE_TIME;
    r->timestamp = htonl(s_unTimestamp);
    r->ssrc = htonl(s_unSsrc);
    return 0;
}
static int build_rtp_nalu(unsigned char *inbuffer, int frame_size, int cur_conn_num) {
    RTP_header rtp_header;
    int data_left;
    int ret;
    unsigned char nalu_header;
    unsigned char fu_indic;
    unsigned char fu_header;
    unsigned char *p_nalu_data;
    int fu_start = 1;
    int fu_end = 0;
    int count = 0;
    if (!inbuffer) {
        return -1;
    }
    memset(s_pNalu_buffer, 0x0, sizeof(s_pNalu_buffer));
    build_rtp_header(&rtp_header);
    data_left = frame_size - NALU_INDIC_SIZE;
    p_nalu_data = inbuffer + NALU_INDIC_SIZE;
    //Single RTP Packet.
    if (data_left <= SINGLE_NALU_DATA_MAX) {
        rtp_header.seq_no = htons(s_nSeqNum++);
        rtp_header.marker = 1;
        memcpy(s_pNalu_buffer, &rtp_header, sizeof(rtp_header));
        memcpy(s_pNalu_buffer + RTP_HEADER_SIZE, p_nalu_data, data_left);
        //ret = write(cur_conn_num,&nalu_buffer[0], data_left + RTP_HEADER_SIZE);
        print("send1 to %d \n", ret);
        ret = sendto(cur_conn_num, s_pNalu_buffer, data_left + RTP_HEADER_SIZE, 0,
                     (struct sockaddr *) &s_stServAddrRtp, sizeof(s_stServAddrRtp));;
        if (ret != data_left + RTP_HEADER_SIZE) {
            print("warning...[%d/%d]\n", ret, data_left + RTP_HEADER_SIZE);
        }
        usleep(DE_TIME);
        return 0;
    }
    //FU-A RTP Packet.
    nalu_header = inbuffer[4];
    fu_indic = (nalu_header & 0xE0) | 28;
    data_left -= NALU_HEAD_SIZE;
    p_nalu_data += NALU_HEAD_SIZE;
    while (data_left > 0) {
        int proc_size = MIN(data_left, SLICE_NALU_DATA_MAX);
        int rtp_size = proc_size + RTP_HEADER_SIZE + FU_A_HEAD_SIZE + FU_A_INDI_SIZE;
        fu_end = (proc_size == data_left);
        fu_header = nalu_header & 0x1F;
        if (fu_start)
            fu_header |= 0x80;
        else if (fu_end)
            fu_header |= 0x40;
        rtp_header.seq_no = htons(s_nSeqNum++);
        memcpy(s_pNalu_buffer, &rtp_header, sizeof(rtp_header));
        memcpy(s_pNalu_buffer + 14, p_nalu_data, proc_size);
        s_pNalu_buffer[12] = fu_indic;
        s_pNalu_buffer[13] = fu_header;
//    ret = write(cur_conn_num,&nalu_buffer[0], rtp_size);
        print("send2 to %d \n", ret);
        ret = sendto(cur_conn_num, s_pNalu_buffer, rtp_size, 0,
                     (struct sockaddr *) &s_stServAddrRtp, sizeof(s_stServAddrRtp));;
        if (fu_end) {
            usleep(DE_TIME * count);
        } else {
            count++;
        }
        if (ret != rtp_size) {
            print("warning...[%d/%d]\n", ret, rtp_size);
        }
        data_left -= proc_size;
        p_nalu_data += proc_size;
        fu_start = 0;
    }
    return 0;
}
static int abstr_nalu_indic(unsigned char *buf, int buf_size, int *be_found) {
    unsigned char *p_tmp;
    int offset;
    int frame_size;
    *be_found = 0;
    offset = 0;
    frame_size = 4;
    p_tmp = buf + 4;
    while (frame_size < buf_size - 4) {
        if (p_tmp[2]) {
            offset = 3;
        } else if (p_tmp[1]) {
            offset = 2;
        } else if (p_tmp[0]) {
            offset = 1;
        } else {
            if (p_tmp[3] != 1) {
                if (p_tmp[3]) {
                    offset = 4;
                } else {
                    offset = 1;
                }
            } else {
                *be_found = 1;
                break;
            }
        }
        frame_size += offset;
        p_tmp += offset;
    }
    if (!*be_found) {
        frame_size = buf_size;
    }
    return frame_size;
}
static unsigned int GetTickCount(void) {
    struct timeval gettick;
    unsigned int tick;
    gettimeofday(&gettick, NULL);
    tick = gettick.tv_sec * 1000 + gettick.tv_usec / 1000;
    return tick;
}
int start(const char *ipaddress) {
    isRunning = true;
    unsigned char message[1024 * 10] = {0x00,};
    unsigned int spend_time;
    int frame_size = 0, bytes_left;
    int found_nalu = 0;
    int clnt_sock, sock_rtp, reuse = 1;
    struct sockaddr_in serv_addr;
    re_try:
    s_unTimestamp = 12345;
    s_unSsrc = 12345;
    s_nSeqNum = 1;
    s_unRtpbufferIndex = 0;
    memset(s_pRtpbuffer, 0x0, sizeof(s_pRtpbuffer));
    clnt_sock = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (clnt_sock == -1) {
        print("socket error.\n");
        return 0;
    }
    sock_rtp = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (sock_rtp == -1) {
        print("socket error.clnt_sock2\n");
        return 0;
    }
    reuse = 1;
    /*set address reuse*/
    (void) setsockopt(clnt_sock, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse));
    (void) setsockopt(sock_rtp, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse));
    memset(&serv_addr, 0x0, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = inet_addr(SRC_IP_ADDR);
    serv_addr.sin_port = htons(SRC_PORT);
    memset(&s_stServAddrRtp, 0x0, sizeof(s_stServAddrRtp));
    s_stServAddrRtp.sin_family = AF_INET;
    if (ipaddress != NULL) {
        s_stServAddrRtp.sin_addr.s_addr = inet_addr(ipaddress);
        print("Dest IP:%s\n", ipaddress);
    } else {
        s_stServAddrRtp.sin_addr.s_addr = inet_addr(DEST_IP_ADDR);
        print("Dest IP:%s\n", DEST_IP_ADDR);
    }
    s_stServAddrRtp.sin_port = htons(DEST_PORT);
    re_connect:
    print("[%s:%d] connecting..\n", SRC_IP_ADDR, SRC_PORT);
    if (connect(clnt_sock, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) == -1) {
        print("connect error..\n");
        if (!isRunning) {
            close(clnt_sock);
            close(sock_rtp);
            return 0;
        }
        sleep(1);
        goto re_connect;
    }
    print("[%s:%d] connecting success!\n", SRC_IP_ADDR, SRC_PORT);
    spend_time = GetTickCount();
    while (isRunning) {
        memset(message, 0x0, sizeof(message));
//        bytes_left = read(clnt_sock,message,sizeof(message)-1);
        bytes_left = recv(clnt_sock, message, sizeof(message) - 1, MSG_DONTWAIT);
        if (bytes_left > 0) {
            spend_time = GetTickCount();
            if (s_unRtpbufferIndex >= PAYLOAD_SIZE_MAX) {
                s_unRtpbufferIndex = 0;
                memset(s_pRtpbuffer, 0x0, sizeof(s_pRtpbuffer));
                print("something wrong...!\n\n");
            }
            memcpy(&s_pRtpbuffer[s_unRtpbufferIndex], message, bytes_left);
            s_unRtpbufferIndex += bytes_left;
            while (bytes_left > 0) {
                found_nalu = 0;
                frame_size = abstr_nalu_indic(s_pRtpbuffer, s_unRtpbufferIndex, &found_nalu);
                if (found_nalu) {
                    print("[0x%x][0x%x][0x%x][0x%x][0x%x]\n", s_pRtpbuffer[0], s_pRtpbuffer[1],
                          s_pRtpbuffer[2], s_pRtpbuffer[3], s_pRtpbuffer[4]);
                    build_rtp_nalu(s_pRtpbuffer, frame_size, sock_rtp);
                    s_unRtpbufferIndex -= frame_size;
                    memset(s_pTempbuffer, 0x0, sizeof(s_pTempbuffer));
                    memcpy(s_pTempbuffer, &s_pRtpbuffer[frame_size], s_unRtpbufferIndex);
                    memcpy(s_pRtpbuffer, s_pTempbuffer, sizeof(s_pRtpbuffer));
                }
                bytes_left -= frame_size;
            }
        } else {
            if (GetTickCount() - spend_time > 3000) {
                close(clnt_sock);
                close(sock_rtp);
                print("connection err..\n");
                goto re_try;
            }
            usleep(1000);
        }
    }
    print("exit..\n");
    close(clnt_sock);
    close(sock_rtp);
    return 0;
}
void stop() {
    isRunning = false;
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_humaxdigital_rtplib_RTPService_start(
        JNIEnv *env,
        jobject /* this */,
        jstring jsIP) {
    print("start..\n");
    const char *ipaddress = env->GetStringUTFChars(jsIP, 0);
    env->ReleaseStringUTFChars(jsIP, ipaddress);

    char *amessage = "nullptr";
    start(ipaddress);
    return nullptr;
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_humaxdigital_rtplib_RTPService_stop(
        JNIEnv *env,
        jobject /* this */) {
    print("stop..\n");
    stop();
    return nullptr;
}