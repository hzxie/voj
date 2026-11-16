/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0600
#endif

#include <string>
#include <vector>

#include <windows.h>

#include "../org_verwandlung_voj_jni_library.h"
#include "../org_verwandlung_voj_judger_util_FileSystemUtils.h"

/**
 * JNI call entry point.
 * Gets the name of the account the judger runs as: the user of its process's token, qualified with
 * its domain (e.g. "HOST\judger", "NT AUTHORITY\SYSTEM" or "NT SERVICE\voj-judger"), so that
 * looking it up finds that account, unlike the bare user.name of Java, which names a domain account
 * and a local one alike, and is the computer's account ("HOST$") for SYSTEM.
 * @param  jniEnv - a reference to the JNI runtime environment
 * @param  jClass - the calling Java class
 * @return the account's name, or NULL with an IllegalStateException pending
 */
JNIEXPORT jstring JNICALL Java_org_verwandlung_voj_judger_util_FileSystemUtils_getProcessAccountName(
    JNIEnv* jniEnv, jclass jClass) {
    HANDLE hToken = NULL;
    BYTE   tokenUser[sizeof(TOKEN_USER) + SECURITY_MAX_SID_SIZE];
    DWORD  size   = 0;
    if ( !OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &hToken) ) {
        throwStringException(jniEnv, "OpenProcessToken failed: " + std::to_string(GetLastError()));
        return NULL;
    }
    BOOL  queried = GetTokenInformation(hToken, TokenUser, tokenUser, sizeof(tokenUser), &size);
    DWORD error   = GetLastError();
    CloseHandle(hToken);
    if ( !queried ) {
        throwStringException(jniEnv, "GetTokenInformation failed: " + std::to_string(error));
        return NULL;
    }
    PSID         sid        = ((PTOKEN_USER) tokenUser)->User.Sid;
    DWORD        nameLength = 0;
    DWORD        domainLength = 0;
    SID_NAME_USE use;
    LookupAccountSidW(NULL, sid, NULL, &nameLength, NULL, &domainLength, &use);
    std::vector<wchar_t> name(nameLength + 1);
    std::vector<wchar_t> domain(domainLength + 1);
    if ( nameLength == 0 || !LookupAccountSidW(NULL, sid, name.data(), &nameLength, domain.data(),
            &domainLength, &use) ) {
        throwStringException(jniEnv, "LookupAccountSid failed: " + std::to_string(GetLastError()));
        return NULL;
    }
    // Well-known accounts such as Everyone have no domain.
    std::wstring account = domain[0] == L'\0' ? std::wstring(name.data())
                                              : std::wstring(domain.data()) + L"\\" + name.data();
    // A Java string is UTF-16, as a wide string on Windows.
    return jniEnv->NewString(reinterpret_cast<const jchar*>(account.c_str()), (jsize) account.size());
}
