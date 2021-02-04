#include <jni.h>
#include <string>
#include <vector>
#include <iostream>

#include <ctime>
#include <dlib/image_processing/frontal_face_detector.h>
#include <opencv2/core/types_c.h>
#include <opencv2/flann/any.h>
#include <dlib/image_processing/shape_predictor.h>
#include <opencv2/imgproc.hpp>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <opencv2/imgproc/types_c.h>
#include <dlib/opencv/cv_image.h>

#define LOG_TAG "MY_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG ,__VA_ARGS__) // 定义LOGD，可以打log

dlib::frontal_face_detector detector = dlib::get_frontal_face_detector();;
dlib::shape_predictor pose_model;

struct membuf : std::streambuf {
    membuf(char *begin, char *end) {
        this->setg(begin, begin, end);
    }
};

extern "C" JNIEXPORT void JNICALL Java_com_example_app7_MainActivity_faceinit(
        JNIEnv *env, jclass obj, jobject assetManager) {
    //首先从Assets中打开特征点识别所需模型
    //获取AAssetManager
    AAssetManager *native_asset = AAssetManager_fromJava(env, assetManager);

    //打开文件
    AAsset *assetFile = AAssetManager_open(native_asset, "shape_predictor_68_face_landmarks.dat",
                                           AASSET_MODE_BUFFER);
    //获取文件长度
    size_t file_length = static_cast<size_t>(AAsset_getLength(assetFile));
    char *model_buffer = (char *) malloc(file_length);
    //读文件
    AAsset_read(assetFile, model_buffer, file_length);
    //关闭文件
    AAsset_close(assetFile);

    //char* to istream
    membuf mem_buf(model_buffer, model_buffer + file_length);
    std::istream in(&mem_buf);

    //dlib读取模型
    dlib::deserialize(pose_model, in);
}

extern "C" JNIEXPORT jobject JNICALL Java_com_example_app7_MainActivity_findface(
        JNIEnv *env, jclass obj, jlong jrgba) {
    //返回Point类型的list
    jclass list_point = env->FindClass("java/util/ArrayList");//ArrayList类
    jmethodID list_init = env->GetMethodID(list_point, "<init>", "()V");//list初始化方法
    jobject list_obj = env->NewObject(list_point, list_init);//new一个list_point对象
    jmethodID list_add = env->GetMethodID(list_point, "add", "(Ljava/lang/Object;)Z");//list的add方法
    jclass point = env->FindClass("org/opencv/core/Point");//Point类
    jmethodID point_init = env->GetMethodID(point, "<init>", "(DD)V");//Point初始化方法

    //从内存地址获取帧图像
    cv::Mat &frame = *(cv::Mat *) jrgba;
    cv::Mat img;

    //下采样两次
    pyrDown(frame, img, cv::Size(frame.cols / 2, frame.rows / 2));
    pyrDown(img, img, cv::Size(img.cols / 2, img.rows / 2));

    //opencv Mat to dlib array2d
    dlib::array2d<dlib::bgr_pixel> cimg(img.rows, img.cols);
    for (int i = 0; i < img.rows; i++) {
        for (int j = 0; j < img.cols; j++) {
            cimg[i][j].blue = img.at<cv::Vec3b>(i, j)[0];
            cimg[i][j].green = img.at<cv::Vec3b>(i, j)[1];
            cimg[i][j].red = img.at<cv::Vec3b>(i, j)[2];
        }
    }

    //人脸识别
    std::vector<dlib::rectangle> faces = detector(cimg);
    std::vector<dlib::full_object_detection> shapes;

    //特征点识别
    for (auto &face : faces)
        shapes.push_back(pose_model(cimg, face));

    if (!shapes.empty()) {
        for (int i = 0; i < 68; i++) {
            //landmark每个点的坐标
            long x = shapes[0].part(i).x() * 3;
            long y = shapes[0].part(i).y() * 4;
            cv::circle(frame, cvPoint(x, y), 3, cv::Scalar(0, 255, 0), -1);//画图
            jobject jobj = env->NewObject(point, point_init, (double) x, (double) y);//new一个Point
            env->CallBooleanMethod(list_obj, list_add, jobj);//add到list中
        }
    }

    return list_obj;
}