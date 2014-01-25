/*
 * logstream.h
 * Thanks to github user dzhioev!
 */

#ifndef LOGSTREAM_H_
#define LOGSTREAM_H_

#include <iosfwd>
#include <sstream>
#include <android/log.h>

template <enum android_LogPriority P>
class androidbuf : public std::streambuf {
public:
    enum { bufsize = 256 }; // ... or some other suitable buffer size
    androidbuf(const char *tag) : m_tag(tag) {
    	setp(buffer, buffer + bufsize - 1);
    }
private:
    int overflow(int c) {
        if (c == traits_type::eof()) {
            *pptr() = traits_type::to_char_type(c);
            sbumpc();
        }
        return sync()? traits_type::eof(): traits_type::not_eof(c);
    }
    int sync() {
        if (pbase() != pptr()) {
            __android_log_write(P, m_tag, std::string(pbase(), pptr()-pbase()).c_str());
            setp(buffer, buffer + bufsize - 1);
        }
        return 0;
    }
    char buffer[bufsize];
    const char *m_tag;
};

template <enum android_LogPriority P>
class logstream : public std::ostream {
public:
	explicit logstream(const char *tag) : std::ostream(m_logbuf = new androidbuf<P>(tag)) {
	}
	~logstream() {
		delete m_logbuf;
	}
private:
	androidbuf<P> *m_logbuf;
};

#endif /* LOGSTREAM_H_ */
