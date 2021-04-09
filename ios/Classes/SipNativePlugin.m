#import "SipNativePlugin.h"
#if __has_include(<sip_native/sip_native-Swift.h>)
#import <sip_native/sip_native-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "sip_native-Swift.h"
#endif

@implementation SipNativePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftSipNativePlugin registerWithRegistrar:registrar];
}
@end
