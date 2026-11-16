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

#include <windows.h>

#include "../org_verwandlung_voj_judger_util_FileSystemUtils.h"

/**
 * The privileges that let the judger remove whatever a submission leaves in its work directory,
 * whatever its ACL: SeBackupPrivilege lets it list a directory and read attributes, and
 * SeRestorePrivilege lets it delete, whenever a file is opened for backup (FILE_FLAG_BACKUP_SEMANTICS,
 * which is how Windows opens a file to delete it, a directory to list it, and Java a file to read
 * its attributes); SeTakeOwnershipPrivilege lets it take a file it may not change the ACL of.
 * Administrators hold them, disabled until a process enables them.
 * See https://learn.microsoft.com/windows/win32/secauthz/privilege-constants
 */
static const wchar_t* const BACKUP_PRIVILEGES[] = {
    L"SeBackupPrivilege", L"SeRestorePrivilege", L"SeTakeOwnershipPrivilege",
};

/**
 * JNI call entry point.
 * Enables the backup privileges (BACKUP_PRIVILEGES) of the judger's process, for every thread of
 * it. They stay enabled until it exits.
 * @param  jniEnv - a reference to the JNI runtime environment
 * @param  jClass - the calling Java class
 * @return whether all of them are enabled: false if the judger does not hold one of them (it does
 *         not run as an elevated administrator)
 */
JNIEXPORT jboolean JNICALL Java_org_verwandlung_voj_judger_util_FileSystemUtils_enableBackupPrivileges(
    JNIEnv* jniEnv, jclass jClass) {
    HANDLE hToken = NULL;
    if ( !OpenProcessToken(GetCurrentProcess(), TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &hToken) ) {
        return JNI_FALSE;
    }
    bool enabled = true;
    for ( const wchar_t* name : BACKUP_PRIVILEGES ) {
        TOKEN_PRIVILEGES privileges = {0};
        privileges.PrivilegeCount           = 1;
        privileges.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
        // AdjustTokenPrivileges succeeds for a privilege the token does not hold, but reports
        // ERROR_NOT_ALL_ASSIGNED.
        if ( !LookupPrivilegeValueW(NULL, name, &privileges.Privileges[0].Luid) ||
                !AdjustTokenPrivileges(hToken, FALSE, &privileges, sizeof(privileges), NULL, NULL) ||
                GetLastError() == ERROR_NOT_ALL_ASSIGNED ) {
            enabled = false;
        }
    }
    CloseHandle(hToken);
    return enabled ? JNI_TRUE : JNI_FALSE;
}
