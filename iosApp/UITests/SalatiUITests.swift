import XCTest
final class SalatiUITests:XCTestCase {
    func testChooseOfflineCityAndNavigate() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset"];app.launch()
        app.buttons["chooseLocation"].tap()
        let search=app.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout:10));search.tap();search.typeText("Damascus")
        let city=app.buttons.matching(NSPredicate(format:"identifier BEGINSWITH 'city.'")).firstMatch
        XCTAssertTrue(city.waitForExistence(timeout:10));city.tap()
        let proceed=app.buttons["continueOnboarding"]
        app.swipeUp();XCTAssertTrue(proceed.waitForExistence(timeout:5));proceed.tap()
        XCTAssertTrue(app.tabBars.buttons["Calendar"].waitForExistence(timeout:10))
        capture("Prayer home")
        app.tabBars.buttons["Calendar"].tap();capture("Hijri calendar")
        app.tabBars.buttons["Qibla"].tap();capture("Qibla without simulator heading")
        app.tabBars.buttons["Settings"].tap();capture("Settings")
    }
    func testArabicDarkOfflineFlow() {
        let app=XCUIApplication();app.launchArguments=["-uiTestingReset","-uiTestingArabicDark"];app.launch()
        app.buttons["chooseLocation"].tap()
        let search=app.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout:10));search.tap();search.typeText("Madinah")
        let city=app.buttons.matching(NSPredicate(format:"identifier BEGINSWITH 'city.'")).firstMatch
        XCTAssertTrue(city.waitForExistence(timeout:10));city.tap()
        app.swipeUp();app.buttons["continueOnboarding"].tap()
        XCTAssertTrue(app.tabBars.buttons["التقويم"].waitForExistence(timeout:10))
        capture("Arabic dark prayer home")
        let text=app.staticTexts.allElementsBoundByIndex.map(\.label).joined(separator:" ")
        XCTAssertNil(text.range(of:"[٠-٩۰-۹]",options:.regularExpression),"Western digits must remain independent of Arabic UI")
        app.tabBars.buttons["التقويم"].tap();capture("Arabic dark Hijri calendar")
        app.tabBars.buttons["القبلة"].tap();capture("Arabic dark Qibla")
        app.tabBars.buttons["الإعدادات"].tap();capture("Arabic dark Settings")
    }
    func capture(_ name:String) {let attachment=XCTAttachment(screenshot:XCUIApplication().screenshot());attachment.name=name;attachment.lifetime = .keepAlways;add(attachment)}
}
