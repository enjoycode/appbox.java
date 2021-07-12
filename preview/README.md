本目录内存放用于预览的临时编译的公共flutter包,需要打包至运行时


# build flutter_web.js
```bash 
/home/rick/snap/flutter/common/flutter/bin/cache/dart-sdk/bin/dartdevc -s /home/rick/snap/flutter/common/flutter/bin/cache/flutter_web_sdk/flutter_web_sdk/kernel/flutter_ddc_sdk_sound.dill --packages=bootstrap/.dart_tool/package_config.json --modules=amd --sound-null-safety --enable-experiment=non-nullable -o flutter_web.js  package:flutter/animation.dart package:flutter/cupertino.dart package:flutter/foundation.dart package:flutter/gestures.dart package:flutter/material.dart package:flutter/painting.dart package:flutter/physics.dart package:flutter/rendering.dart package:flutter/scheduler.dart package:flutter/semantics.dart package:flutter/services.dart package:flutter/widgets.dart
```


```bash
dartdevc -s /Users/lushuaijun/Tests/flutter_web/build2/flutter_web.dill -s /usr/local/opt/flutter/bin/cache/flutter_web_sdk/flutter_web_sdk/kernel/flutter_ddc_sdk_sound.dill --modules=amd --sound-null-safety --enable-experiment=non-nullable --packages=.dart_tool/package_config.json -o build2/main.dart.js lib/main.dart
```
