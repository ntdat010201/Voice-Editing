#pragma once
#include <vector>
#include <mutex>

class PcmBuffer {
public:
    void push(const float* data, int frames);
    int pop(float* out, int frames);
    void clear();

private:
    std::vector<float> buffer_;
    std::mutex mutex_;
};

