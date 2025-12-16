#pragma once
#include "PcmBuffer.h"
#include <cstdint>
#include <atomic>

class VoiceEngine {
public:
    bool start();
    void stop();
    void setPitch(float pitch);
    void pushPcm(const float* data, int frames);
    void clearBuffer();

private:
    PcmBuffer pcmBuffer_;
    std::atomic<float> pitch_{1.0f};
};
