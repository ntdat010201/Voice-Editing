#include "PcmBuffer.h"
#include <algorithm>

void PcmBuffer::push(const float* data, int frames) {
    std::lock_guard<std::mutex> lock(mutex_);
    buffer_.insert(buffer_.end(), data, data + frames * 2);
}

int PcmBuffer::pop(float* out, int frames) {
    std::lock_guard<std::mutex> lock(mutex_);
    int available = buffer_.size() / 2;
    int toRead = std::min(available, frames);
    if (toRead > 0) {
        std::copy(buffer_.begin(), buffer_.begin() + toRead * 2, out);
        buffer_.erase(buffer_.begin(), buffer_.begin() + toRead * 2);
    }
    return toRead;
}

void PcmBuffer::clear() {
    std::lock_guard<std::mutex> lock(mutex_);
    buffer_.clear();
}
