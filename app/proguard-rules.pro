-renamesourcefileattribute SourceFile

-assumenosideeffects class android.util.Log {
  public static *** v(...);
  public static *** d(...);
  public static *** i(...);
  public static *** w(...);
  public static *** e(...);
}

#-dontwarn androidx.databinding.**
#-keep class androidx.databinding.** { *; }
#-keep class com.example.parus.DataBindingInfo { *; }
#-keep class com.begletsov.parus.databinding.** { *; }
#-keep class com.begletsov.parus.DataBinderMapperImpl { *; }
#-keep class com.begletsov.parus.DataBinderMapper { *; }
#-keep class * extends com.begletsov.parus.DataBinderMapper { *; }
#-keep class * extends com.begletsov.parus.DataBinderMapperImpl { *; }
#-keep class * extends com.begletsov.parus.DataBinderMapperImpl$Inner { *; }
#-keep class com.begletsov.parus.DataBindingInfo { *; }
#-keep class com.begletsov.parus.DataBinderMapperImpl$InnerLayoutIdLookup { *; }
#-keep class com.begletsov.parus.DataBinderMapperImpl$InnerBrLookup { *; }
#-keep class com.begletsov.parus.viewmodels { *; }
#-keep class * extends androidx.fragment.Fragment { *; }