import { ArkUIViewController, Compose } from 'compose';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { sensor } from '@kit.SensorServiceKit';
import nativeApi from 'libentry.so';

@Entry
@Component
struct Index {
  private controller: ArkUIViewController | undefined = undefined
  private sensorType: sensor.SensorId | undefined = undefined
  private readonly sensorCallback = (data: sensor.GravityResponse | sensor.AccelerometerResponse): void => {
    nativeApi.SetOhosGravity(data.x, data.y)
  }

  aboutToAppear(): void {
    try {
      this.controller = nativeApi.MainArkUIViewController() as ArkUIViewController
    } catch (error) {
      hilog.error(0x0000, 'Glass', 'MainArkUIViewController init failed: %{public}s', String(error))
    }
  }

  aboutToDisappear(): void {
    this.stopSensor()
  }

  onPageShow(): void {
    this.controller?.onPageShow()
    this.startSensor()
  }

  onPageHide(): void {
    this.controller?.onPageHide()
    this.stopSensor()
  }

  private startSensor(): void {
    if (this.sensorType !== undefined) {
      return
    }
    try {
      sensor.on(sensor.SensorId.GRAVITY, this.sensorCallback, { interval: 100000000 })
      this.sensorType = sensor.SensorId.GRAVITY
      return
    } catch (error) {
      hilog.warn(0x0000, 'Glass', 'GRAVITY sensor unavailable: %{public}s', String(error))
    }
    try {
      sensor.on(sensor.SensorId.ACCELEROMETER, this.sensorCallback, { interval: 100000000 })
      this.sensorType = sensor.SensorId.ACCELEROMETER
    } catch (error) {
      hilog.warn(0x0000, 'Glass', 'ACCELEROMETER sensor unavailable: %{public}s', String(error))
    }
  }

  private stopSensor(): void {
    if (this.sensorType === undefined) {
      return
    }
    try {
      if (this.sensorType === sensor.SensorId.GRAVITY) {
        sensor.off(sensor.SensorId.GRAVITY, this.sensorCallback)
      } else if (this.sensorType === sensor.SensorId.ACCELEROMETER) {
        sensor.off(sensor.SensorId.ACCELEROMETER, this.sensorCallback)
      }
    } catch (error) {
      hilog.warn(0x0000, 'Glass', 'stop sensor failed: %{public}s', String(error))
    }
    this.sensorType = undefined
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
