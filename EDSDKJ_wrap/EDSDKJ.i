%module EDSDKPy

%inline %{
#include "EDSDK.h"
%}

%include "cpointer.i"
%include "enums.swg"
%include "arrays_java.i"

// Typedefs
typedef void                    EdsVoid;
typedef int                     EdsBool;
typedef char                    EdsChar;
typedef char                    EdsInt8;
typedef unsigned char           EdsUInt8;
typedef short                   EdsInt16;
typedef unsigned short          EdsUInt16;
typedef long                    EdsInt32;
typedef unsigned long           EdsUInt32;
typedef __int64             EdsInt64;
typedef unsigned __int64    EdsUInt64;
typedef float                   EdsFloat;
typedef double                  EdsDouble;

// Reference Types
typedef struct __EdsObject*    EdsBaseRef;
typedef  EdsBaseRef    EdsCameraListRef;
typedef  EdsBaseRef    EdsCameraRef;
typedef  EdsBaseRef    EdsVolumeRef;
typedef  EdsBaseRef    EdsDirectoryItemRef;
typedef  EdsBaseRef    EdsStreamRef;
typedef  EdsStreamRef  EdsImageRef;
typedef  EdsBaseRef    EdsEvfImageRef ;

// Error Types
typedef EdsUInt32    EdsError;

typedef struct tagEdsDeviceInfo
{
%immutable;
    EdsChar*     szPortName;
    EdsChar*     szDeviceDescription;
    EdsUInt32   deviceSubType;
	EdsUInt32	reserved;
} EdsDeviceInfo;

typedef struct tagEdsPoint
{
    EdsInt32    x;
    EdsInt32    y;
} EdsPoint;

typedef struct tagEdsSize
{
    EdsInt32    width;
    EdsInt32    height;
} EdsSize;

typedef struct tagEdsRect
{
    EdsPoint    point;
    EdsSize     size;
} EdsRect;

typedef struct tagEdsImageInfo
{
    EdsUInt32   width;
    EdsUInt32   height;
    EdsUInt32   numOfComponents;
    EdsUInt32   componentDepth;
    EdsRect     effectiveRect;
    EdsUInt32   reserved1;
    EdsUInt32   reserved2;
} EdsImageInfo;

typedef struct tagEdsVolumeInfo
{
%immutable;
    EdsUInt32   storageType;
    EdsAccess   access;
    unsigned long long   maxCapacity;
    unsigned long long   freeSpaceInBytes;
    EdsChar*     szVolumeLabel;

} EdsVolumeInfo;

typedef struct tagEdsDirectoryItemInfo
{
%immutable;
    EdsUInt32   size;
    EdsBool     isFolder;
    EdsUInt32   groupID;
    EdsUInt32   option;
    EdsChar*     szFileName;
	EdsUInt32	format;
} EdsDirectoryItemInfo;

// Enums

%javaconst(1);
enum EdsImageSource
{
    kEdsImageSrc_FullView       = 0 ,
    kEdsImageSrc_Thumbnail          ,
    kEdsImageSrc_Preview            ,
    kEdsImageSrc_RAWThumbnail       ,
    kEdsImageSrc_RAWFullView        ,

};

enum EdsTargetImageType
{
    kEdsTargetImageType_Unknown = 0x00000000,
    kEdsTargetImageType_Jpeg    = 0x00000001,
    kEdsTargetImageType_TIFF    = 0x00000007,
    kEdsTargetImageType_TIFF16  = 0x00000008,
    kEdsTargetImageType_RGB     = 0x00000009,
    kEdsTargetImageType_RGB16   = 0x0000000A,
    kEdsTargetImageType_DIB     = 0x0000000B

};

enum EdsSeekOrigin
{
    kEdsSeek_Cur     = 0,
    kEdsSeek_Begin      ,
    kEdsSeek_End        ,

};

enum EdsAccess
{
    kEdsAccess_Read          = 0,
    kEdsAccess_Write            ,
    kEdsAccess_ReadWrite        ,
    kEdsAccess_Error         = 0xFFFFFFFF,

};

enum EdsFileCreateDisposition
{
    kEdsFileCreateDisposition_CreateNew          = 0,
    kEdsFileCreateDisposition_CreateAlways          ,
    kEdsFileCreateDisposition_OpenExisting          ,
    kEdsFileCreateDisposition_OpenAlways            ,
    kEdsFileCreateDisposition_TruncateExsisting     ,

};

%inline %{
EdsSize createEdsSize(int width, int height) {
	EdsSize sz;
	sz.width = width;
	sz.height = height;
	return sz;
}

EdsPoint createEdsPoint(int x, int y) {
	EdsPoint pt;
	pt.x = x;
	pt.y = y;
	return pt;
}

EdsRect createEdsRect(int x, int y, int width, int height) {
	EdsRect rc;
	rc.point = createEdsPoint(x, y);
	rc.size = createEdsSize(width, height);
	return rc;
}
%}

#define EDS_ERR_OK 0

%pointer_class(EdsBaseRef, BaseRef);
%pointer_class(EdsUInt32, UInt32Ref);

EdsError EdsInitializeSDK();
EdsError EdsTerminateSDK();
EdsUInt32 EdsRetain(EdsBaseRef inRef);
EdsUInt32 EdsRelease(EdsBaseRef inRef);
EdsError EdsGetChildCount(EdsBaseRef inRef, EdsUInt32* outCount);
EdsError EdsGetChildAtIndex(EdsBaseRef inRef, EdsInt32 inIndex, EdsBaseRef* outRef);
EdsError EdsGetCameraList(EdsBaseRef* outCameraListRef);
EdsError EdsOpenSession(EdsCameraRef inCameraRef);
EdsError EdsCloseSession(EdsCameraRef inCameraRef);
EdsError EdsGetDeviceInfo(EdsCameraRef inCameraRef, EdsDeviceInfo* outDeviceInfo);
EdsError EdsGetVolumeInfo(EdsVolumeRef inVolumeRef, EdsVolumeInfo* outVolumeInfo);
EdsError EdsGetDirectoryItemInfo(EdsDirectoryItemRef inDirItemRef, EdsDirectoryItemInfo* outDirItemInfo);
EdsError EdsCreateMemoryStream(EdsUInt32 inBufferSize, EdsStreamRef* outStream);
EdsError EdsDownload(EdsDirectoryItemRef inDirItemRef, EdsUInt32 inReadSize, EdsStreamRef outStream);
EdsError EdsDownloadCancel(EdsDirectoryItemRef inDirItemRef);
EdsError EdsDownloadComplete(EdsDirectoryItemRef inDirItemRef);
EdsError EdsGetImageInfo(EdsImageRef inImageRef, EdsImageSource inImageSource, EdsImageInfo* outImageInfo);
EdsError EdsGetImage(
        EdsImageRef             inImageRef,
        EdsImageSource          inImageSource,
        EdsTargetImageType      inImageType,
        EdsRect                 inSrcRect,
        EdsSize                 inDstSize,
        EdsStreamRef            outStreamRef );
%apply (char *STRING, size_t LENGTH) { (EdsVoid* outBuffer, size_t outBufferSize) }
%inline %{
EdsError EdsReadFromStream(EdsStreamRef inStreamRef, EdsUInt32 inReadSize,
				EdsVoid* outBuffer, size_t outBufferSize,
				EdsUInt32* outReadSize) {
	return EdsRead(inStreamRef, inReadSize, outBuffer, outReadSize);
}
%}
%apply (char *STRING, size_t LENGTH) { (EdsVoid* inBuffer, size_t inBufferSize) }
%inline %{
EdsError EdsWriteToStream(EdsStreamRef inStreamRef, EdsUInt32 inWriteSize,
				EdsVoid* inBuffer, size_t inBufferSize,
				EdsUInt32* outReadSize) {
	return EdsWrite(inStreamRef, inWriteSize, inBuffer, outReadSize);
}
%}
EdsError EdsSeek(
                EdsStreamRef            inStreamRef,
                EdsInt32                inSeekOffset,
                EdsSeekOrigin           inSeekOrigin );
EdsError EdsGetPosition(
                EdsStreamRef            inStreamRef,
                EdsUInt32*              outPosition );
EdsError EdsGetLength(
                EdsStreamRef            inStreamRef,
                EdsUInt32*              outLength );
EdsError EdsCreateFileStream(
                            const EdsChar*              inFileName,
                            EdsFileCreateDisposition    inCreateDisposition,
                            EdsAccess                   inDesiredAccess,
                            EdsStreamRef*               outStream );			
EdsError EdsCreateImageRef(EdsStreamRef inStreamRef, EdsImageRef* outImageRef);