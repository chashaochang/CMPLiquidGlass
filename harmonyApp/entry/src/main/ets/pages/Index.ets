import { ArkUIViewController, Compose } from 'compose';
import { hilog } from '@kit.PerformanceAnalysisKit';
import nativeApi from 'libentry.so';

@Entry
@Component
struct Index {
  private controller: ArkUIViewController | undefined = undefined

  aboutToAppear(): void {
    try {
      this.controller = nativeApi.MainArkUIViewController() as ArkUIViewController
    } catch (error) {
      hilog.error(0x0000, 'Glass', 'MainArkUIViewController init failed: %{public}s', String(error))
    }
  }

  onPageShow(): void {
    this.controller?.onPageShow()
  }

  onPageHide(): void {
    this.controller?.onPageHide()
  }

  onBackPress(): boolean | void {
    return this.controller ? this.controller.onBackPress() : false
  }

  build() {
    Stack() {
      if (this.controller) {
        Compose({
          controller: this.controller,
          libraryName: 'entry',
          onBackPressed: () => this.controller ? this.controller.onBackPress() : false
        })
          .width('100%')
          .height('100%')
      }
    }
    .width('100%')
    .height('100%')
  }
}
