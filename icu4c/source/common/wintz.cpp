// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
********************************************************************************
*   Copyright (C) 2005-2015, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File WINTZ.CPP
*
********************************************************************************
*/

#include "unicode/utypes.h"

// This file contains only desktop Windows behavior
// Windows UWP calls Windows::Globalization directly, so this isn't needed there.
#if U_PLATFORM_USES_ONLY_WIN32_API || U_PLATFORM_HAS_WINUWP_API == 0

#include "wintz.h"
#include "cmemory.h"
#include "cstring.h"
#include "cwchar.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"

#ifndef WIN32_LEAN_AND_MEAN
#   define WIN32_LEAN_AND_MEAN
#endif
#   define VC_EXTRALEAN
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#include <windows.h>

#define MAX_LENGTH_ID 40

/*
  This code attempts to detect the Windows time zone directly,
  as set in the Windows Date and Time control panel.  It attempts
  to work on versions greater than Windows Vista and on localized
  installs.  It works by directly interrogating the registry and
  comparing the data there with the data returned by the
  GetTimeZoneInformation API, along with some other strategies.  The
  registry contains time zone data under this key:

    HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Time Zones\

  Under this key are several subkeys, one for each time zone.  For
  example these subkeys are named "Pacific Standard Time" on Vista+.
  There are some other wrinkles; see the code for
  details.  The subkey name is NOT LOCALIZED, allowing us to support
  localized installs.

  Under the subkey are data values.  We care about:

    Std   Standard time display name, localized
    TZI   Binary block of data

  The TZI data is of particular interest.  It contains the offset, two
  more offsets for standard and daylight time, and the start and end
  rules.  This is the same data returned by the GetTimeZoneInformation
  API.  The API may modify the data on the way out, so we have to be
  careful, but essentially we do a binary comparison against the TZI
  blocks of various registry keys.  When we find a match, we know what
  time zone Windows is set to.  Since the registry key is not
  localized, we can then translate the key through a simple table
  lookup into the corresponding ICU time zone.

  This strategy doesn't always work because there are zones which
  share an offset and rules, so more than one TZI block will match.
  For example, both Tokyo and Seoul are at GMT+9 with no DST rules;
  their TZI blocks are identical.  For these cases, we fall back to a
  name lookup.  We attempt to match the display name as stored in the
  registry for the current zone to the display name stored in the
  registry for various Windows zones.  By comparing the registry data
  directly we avoid conversion complications.

  Author: Alan Liu
  Since: ICU 2.6
  Based on original code by Carl Brown <cbrown@xnetinc.com>
*/

/**
 * Main Windows time zone detection function.  Returns the Windows
 * time zone, translated to an ICU time zone, or NULL upon failure.
 */
U_CFUNC const char* U_EXPORT2
uprv_detectWindowsTimeZone() 
{
    UErrorCode status = U_ZERO_ERROR;
    UResourceBundle* bundle = NULL;
    char* icuid = NULL;
    char apiStdName[MAX_LENGTH_ID];
    char regStdName[MAX_LENGTH_ID];
    char tmpid[MAX_LENGTH_ID];
    size_t len;
    int id;
    int errorCode;
    wchar_t ISOcodeW[3]; /* 2 letter iso code in UTF-16*/
    char  ISOcodeA[3]; /* 2 letter iso code in ansi */

    LONG result;
    DYNAMIC_TIME_ZONE_INFORMATION apiTZI;

    BOOL tryPreVistaFallback;
    OSVERSIONINFO osVerInfo;

    /* Obtain TIME_ZONE_INFORMATION from the API, and then convert it
       to TZI.  We could also interrogate the registry directly; we do
       this below if needed. */
    uprv_memset(&apiTZI, 0, sizeof(apiTZI));
    GetDynamicTimeZoneInformation(&apiTZI);

    /*
     * Copy the timezone ID to icuid to be returned.
     */
    if (apiTZI.TimeZoneKeyName[0] != 0)
    {
        len = uprv_wcslen(apiTZI.TimeZoneKeyName);
        icuid = (char*)uprv_calloc(len + 1, sizeof(char));
        if (icuid != NULL) 
        {
            uprv_wcstombs(icuid, apiTZI.TimeZoneKeyName, len + 1);
        }
    }

    ures_close(bundle);
    
    return icuid;
}

#endif /* U_PLATFORM_USES_ONLY_WIN32_API || U_PLATFORM_HAS_WINUWP_API == 0 */
